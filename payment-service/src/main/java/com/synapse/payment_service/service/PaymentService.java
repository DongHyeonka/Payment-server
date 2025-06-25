package com.synapse.payment_service.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.payment_service.domain.Order;
import com.synapse.payment_service.domain.Subscription;
import com.synapse.payment_service.domain.enums.PaymentStatus;
import com.synapse.payment_service.domain.enums.SubscriptionTier;
import com.synapse.payment_service.dto.IamportResponse;
import com.synapse.payment_service.dto.PaymentData;
import com.synapse.payment_service.dto.request.PaymentRequestDto;
import com.synapse.payment_service.dto.request.PaymentVerificationRequest;
import com.synapse.payment_service.dto.response.PaymentPreparationResponse;
import com.synapse.payment_service.exception.ExceptionCode;
import com.synapse.payment_service.exception.NotFoundException;
import com.synapse.payment_service.repository.OrderRepository;
import com.synapse.payment_service.repository.SubscriptionRepository;
import com.synapse.payment_service.service.converter.PaymentStatusConverter;

import io.portone.sdk.server.PortOneClient;
import io.portone.sdk.server.payment.Payment;
import io.portone.sdk.server.webhook.WebhookVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final SubscriptionRepository subscriptionRepository;
    private final OrderRepository orderRepository;
    private final PortOneClient portOneClient;
    private final PaymentStatusConverter paymentStatusConverter;
    private final WebhookVerifier webhookVerifier; // 웹훅 검증용

    @Transactional
    public PaymentPreparationResponse preparePayment(UUID memberId, PaymentRequestDto request) {
        SubscriptionTier tier = SubscriptionTier.valueOf(request.tier().toUpperCase());
        BigDecimal amount = tier.getMonthlyPrice();
        String orderName = tier.getTierName() + "_" + "구독";
        String merchantUid = orderName + "_" + UUID.randomUUID();

        Subscription subscription = subscriptionRepository.findByMemberId(memberId)
            .orElseThrow(() -> new NotFoundException(ExceptionCode.SUBSCRIPTION_NOT_FOUND));

        Order order = Order.builder()
                .subscription(subscription)
                .merchantUid(merchantUid)
                .amount(amount)
                .status(PaymentStatus.PENDING)
                .build();

        orderRepository.save(order);

        return new PaymentPreparationResponse(merchantUid, orderName, amount);
    }

    @Transactional
    public void verifyAndProcess(PaymentVerificationRequest request) {
        Order order = orderRepository.findByMerchantUid(request.merchantUid())
            .orElseThrow(() -> new NotFoundException(ExceptionCode.ORDER_NOT_FOUND));
        
        if (order.getStatus() != PaymentStatus.PENDING) {
            log.info("이미 처리된 결제입니다. merchantUid={}", order.getMerchantUid());
            return;
        }

        Payment payment = portOneClient.getPayment().getPayment(request.impUid()).join();
        
        if (payment == null) {
            throw new RuntimeException("포트원 결제 정보 조회 실패");
        }

        // 아임포트 결제 ID 설정
        order.updateIamportUid(request.impUid());

        if (!(payment instanceof Payment.Recognized recognizedPayment)) {
            throw new RuntimeException("결제 정보를 인식할 수 없습니다");
        }

        if (order.getAmount().compareTo(BigDecimal.valueOf(recognizedPayment.getAmount().getTotal())) != 0) {
            log.error("결제 금액 불일치. 주문금액={}, 실제결제금액={}, merchantUid={}", 
                    order.getAmount(), recognizedPayment.getAmount().getTotal(), order.getMerchantUid());
            throw new RuntimeException("결제 금액이 불일치합니다. 결제를 취소합니다.");
        }

        // DelegatingConverter를 통해 결제 상태별 처리 위임
        paymentStatusConverter.processPayment(order, payment);
    }

}
