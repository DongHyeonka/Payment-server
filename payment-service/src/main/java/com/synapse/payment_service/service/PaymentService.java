package com.synapse.payment_service.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.payment_service.domain.Order;
import com.synapse.payment_service.domain.Subscription;
import com.synapse.payment_service.domain.enums.PaymentStatus;
import com.synapse.payment_service.domain.enums.SubscriptionTier;
import com.synapse.payment_service.dto.request.PaymentRequestDto;
import com.synapse.payment_service.dto.request.PaymentVerificationRequest;
import com.synapse.payment_service.dto.request.PaymentWebhookRequest;
import com.synapse.payment_service.dto.response.PaymentPreparationResponse;
import com.synapse.payment_service.exception.ExceptionCode;
import com.synapse.payment_service.exception.NotFoundException;
import com.synapse.payment_service.exception.PaymentVerificationException;
import com.synapse.payment_service.repository.OrderRepository;
import com.synapse.payment_service.repository.SubscriptionRepository;
import com.synapse.payment_service.service.converter.PaymentStatusConverter;

import io.portone.sdk.server.PortOneClient;
import io.portone.sdk.server.payment.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final SubscriptionRepository subscriptionRepository;
    private final OrderRepository orderRepository;
    private final PortOneClient portOneClient;
    private final PaymentStatusConverter paymentStatusConverter;
    private final ObjectMapper objectMapper;

    @Transactional
    public PaymentPreparationResponse preparePayment(UUID memberId, PaymentRequestDto request) {
        SubscriptionTier tier = SubscriptionTier.valueOf(request.tier().toUpperCase());
        BigDecimal amount = tier.getMonthlyPrice();
        String orderName = tier.getTierName() + "_" + "subscription";
        String paymentId = orderName + "_" + UUID.randomUUID();

        Subscription subscription = subscriptionRepository.findByMemberId(memberId)
            .orElseThrow(() -> new NotFoundException(ExceptionCode.SUBSCRIPTION_NOT_FOUND)); // 현재 인증 서버와 연동이 안되어있기 때문에 테스트로 검증

        Order order = Order.builder()
                .subscription(subscription)
                .paymentId(paymentId)
                .amount(amount)
                .status(PaymentStatus.PENDING)
                .build();

        orderRepository.save(order);

        return new PaymentPreparationResponse(paymentId, orderName, amount);
    }

    @Transactional
    public void verifyAndProcess(PaymentVerificationRequest request) {
        processPaymentVerification(request.paymentId(), request.iamPortTransactionId());
    }


    // 웹 훅 용입니다.
    @Transactional
    public void verifyAndProcessWebhook(String requestBody) throws IOException {
        PaymentWebhookRequest webhookRequest = PaymentWebhookRequest.from(requestBody, objectMapper);
        processPaymentVerification(webhookRequest.paymentId(), webhookRequest.transactionId());
    }

    private void processPaymentVerification(String paymentId, String iamPortTransactionId) {
        Order order = orderRepository.findByPaymentId(paymentId)
            .orElseThrow(() -> new NotFoundException(ExceptionCode.ORDER_NOT_FOUND));
        
        if (order.getStatus() != PaymentStatus.PENDING) {
            log.info("이미 처리된 결제입니다. paymentId={}", order.getPaymentId());
            return;
        }

        Payment payment = portOneClient.getPayment().getPayment(iamPortTransactionId).join();
        
        if (payment == null) {
            throw new PaymentVerificationException(ExceptionCode.PAYMENT_VERIFICATION_FAILED);
        }

        // 아임포트 결제 ID 설정
        order.updateIamPortTransactionId(iamPortTransactionId);

        if (!(payment instanceof Payment.Recognized recognizedPayment)) {
            throw new PaymentVerificationException(ExceptionCode.PAYMENT_NOT_RECOGNIZED);
        }

        if (order.getAmount().compareTo(BigDecimal.valueOf(recognizedPayment.getAmount().getTotal())) != 0) {
            log.error("결제 금액 불일치. 주문금액={}, 실제결제금액={}, paymentId={}", 
                order.getAmount(), recognizedPayment.getAmount().getTotal(), order.getPaymentId());
            throw new PaymentVerificationException(ExceptionCode.PAYMENT_AMOUNT_MISMATCH);
        }

        paymentStatusConverter.processPayment(order, payment);
    }
}
