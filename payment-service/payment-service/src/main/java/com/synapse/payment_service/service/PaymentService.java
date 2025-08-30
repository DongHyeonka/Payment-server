package com.synapse.payment_service.service;

import java.io.IOException;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapse.payment_service.domain.entity.Order;
import com.synapse.payment_service.domain.entity.Subscription;
import com.synapse.payment_service.domain.enums.PaymentStatus;
import com.synapse.payment_service.domain.enums.SubscriptionTier;
import com.synapse.payment_service.exception.ExceptionCode;
import com.synapse.payment_service.exception.PaymentVerificationException;
import com.synapse.payment_service.exception.UnauthorizedException;
import com.synapse.payment_service.service.convert.PaymentStatusConverter;
import com.synapse.payment_service.service.externalservice.PortOneService;
import com.synapse.payment_service.service.persistence.db.PaymentServiceOrderRepository;
import com.synapse.payment_service.service.persistence.db.PaymentServiceSubscriptionRepository;
import com.synapse.payment_service_api.dto.request.CancelSubscriptionRequest;
import com.synapse.payment_service_api.dto.request.PaymentRequestDto;
import com.synapse.payment_service_api.dto.request.PaymentVerificationRequest;
import com.synapse.payment_service_api.dto.request.PaymentWebhookRequest;
import com.synapse.payment_service_api.dto.response.PaymentPreparationResponse;

import io.portone.sdk.server.payment.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentServiceSubscriptionRepository paymentServiceSubscriptionRepository;
    private final PaymentServiceOrderRepository paymentServiceOrderRepository;
    private final PortOneService portOneService;
    private final PaymentStatusConverter paymentStatusConverter;
    private final ObjectMapper objectMapper;

    @Transactional
    public PaymentPreparationResponse preparePayment(UUID memberId, PaymentRequestDto request) {
        SubscriptionTier tier = SubscriptionTier.fromTier(request.tier()); // -> static 최적화 방식을 통해서 빠른 타입 변환
        Subscription subscription = paymentServiceSubscriptionRepository.findByMemberId(memberId);

        Order order = Order.createForSubscription(subscription, tier);
        paymentServiceOrderRepository.save(order);

        return new PaymentPreparationResponse(order.getPaymentId(), order.getOrderName(), order.getAmount());
    }

    /**
     * 결제 후 검증 api 요청
     */
    @Transactional
    public void verifyAndProcess(PaymentVerificationRequest request, UUID memberId) {
        processPaymentVerification(request.paymentId(), request.iamPortTransactionId(), memberId);
    }

    // 웹 훅 용입니다.
    @Transactional
    public void verifyAndProcessWebhook(String requestBody) throws IOException {
        PaymentWebhookRequest webhookRequest = PaymentWebhookRequest.from(requestBody, objectMapper);
        if (!webhookRequest.isTransactionWebhook()) {
            log.info("처리 대상이 아닌 웹훅 이벤트를 수신했습니다. event={}", webhookRequest.type());
            return;
        }

        String paymentId = webhookRequest.getPaymentId();
        Order order = paymentServiceOrderRepository.findByOrderId(paymentId);
        Subscription subscription = order.getSubscription(); // 단건이든 이후 정기 결제든 subscription 정보 존재함

        if (order.isAlreadyProcessed()) { // isAlreadyProcessed()는 status가 PAID 또는 FAILED인지 확인하는 메서드
            log.warn("이미 최종 처리된 주문에 대한 웹훅을 수신했습니다. 중복 처리를 방지합니다. paymentId={}, status={}", paymentId, order.getStatus());
            return;
        }

        if (webhookRequest.isPaid()) {
            processWebhookPaymentSuccess(order, subscription, webhookRequest);
        } else if (webhookRequest.isFailed()) {
            processWebhookPaymentFailure(order, subscription, webhookRequest);
        }
    }

    private void processWebhookPaymentSuccess(Order order, Subscription subscription, PaymentWebhookRequest webhookRequest) {
        String iamPortTransactionId = webhookRequest.getTransactionId();
        Payment payment = portOneService.payment(iamPortTransactionId);

        // PortOne 조회 결과가 없거나, 인식할 수 없는 결제 정보라면 예외를 발생시켜 트랜잭션을 롤백합니다.
        if (!(payment instanceof Payment.Recognized recognizedPayment)) {
            log.error("웹훅 검증 실패: PortOne 서버에서 결제 정보를 찾을 수 없거나 인식할 수 없습니다. paymentId={}", order.getPaymentId());
            throw new PaymentVerificationException(ExceptionCode.PAYMENT_VERIFICATION_FAILED);
        }

        try {
            order.validatePaymentAmount(recognizedPayment);
        } catch (PaymentVerificationException e) {
            log.error("웹훅 검증 실패: 결제 금액이 일치하지 않습니다. 주문금액={}, 실제결제금액={}, paymentId={}",
                    order.getAmount(), recognizedPayment.getAmount().getTotal(), order.getPaymentId());
            throw e;
        }

        order.updateIamPortTransactionId(iamPortTransactionId);
        order.markAsPaid();

        subscription.renewSubscription(subscription.getTier());

        if (order.hasBillingKey(recognizedPayment)) {
            String billingKey = recognizedPayment.getBillingKey();
            subscription.updateBillingKey(billingKey);
        }
    }

    private void processWebhookPaymentFailure(Order order, Subscription subscription, PaymentWebhookRequest webhookRequest) {
        order.updateStatus(PaymentStatus.FAILED);

        subscription.handlePaymentFailure();
    }

    // 결제 검증 (memberId가 null이면 웹훅용, 아니면 클라이언트용)
    private void processPaymentVerification(String paymentId, String iamPortTransactionId, UUID memberId) {
        Order order = paymentServiceOrderRepository.findByOrderId(paymentId);

        // 클라이언트 요청인 경우에만 권한 검증 (웹훅은 memberId가 null)
        if (memberId != null && !order.getSubscription().getMemberId().equals(memberId)) {
            throw new UnauthorizedException(ExceptionCode.UNAUTHORIZED_USER);
        }

        // 도메인 객체를 통한 중복 처리 검증
        if (order.isAlreadyProcessed()) {
            log.info("이미 처리된 결제입니다. paymentId={}", order.getPaymentId());
            return;
        }

        Payment payment = portOneService.payment(iamPortTransactionId);

        if (payment == null) {
            throw new PaymentVerificationException(ExceptionCode.PAYMENT_VERIFICATION_FAILED);
        }

        // 아임포트 결제 ID 설정
        order.updateIamPortTransactionId(iamPortTransactionId);

        if (!(payment instanceof Payment.Recognized recognizedPayment)) {
            throw new PaymentVerificationException(ExceptionCode.PAYMENT_NOT_RECOGNIZED);
        }

        // 도메인 객체를 통한 결제 금액 검증
        try {
            order.validatePaymentAmount(recognizedPayment);
        } catch (PaymentVerificationException e) {
            log.error(
                "결제 금액 불일치. 주문금액={}, 실제결제금액={}, paymentId={}",
                order.getAmount(), recognizedPayment.getAmount().getTotal(), order.getPaymentId()
            );
            throw e;
        }

        paymentStatusConverter.processPayment(order, payment);

        // 도메인 객체를 통한 빌링키 처리
        if (order.hasBillingKey(recognizedPayment)) {
            Subscription subscription = order.getSubscription();
            String billingKey = recognizedPayment.getBillingKey();
            subscription.updateBillingKey(billingKey); // 더티체킹으로 save 필요 없음
        }
    }

    @Transactional
    public void cancelSubscription(UUID memberId, CancelSubscriptionRequest request) {
        Subscription subscription = paymentServiceSubscriptionRepository.findByMemberId(memberId);

        Order order = paymentServiceOrderRepository.findBySubscriptionId(subscription.getId());

        order.cancel(); // 여기서 더티체킹
        subscription.deactivate(); // 여기서 더티체킹
    }
}
