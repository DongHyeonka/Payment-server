package com.synapse.payment_service.service;

import java.io.IOException;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapse.payment_service.domain.entity.Order;
import com.synapse.payment_service.domain.entity.Subscription;
import com.synapse.payment_service.domain.enums.SubscriptionTier;
import com.synapse.payment_service.service.externalservice.PaymentProcessor;
import com.synapse.payment_service.service.persistence.db.PaymentServiceOrderRepository;
import com.synapse.payment_service.service.persistence.db.PaymentServiceSubscriptionRepository;
import com.synapse.payment_service_api.dto.request.CancelSubscriptionRequest;
import com.synapse.payment_service_api.dto.request.PaymentRequestDto;
import com.synapse.payment_service_api.dto.request.PaymentVerificationRequest;
import com.synapse.payment_service_api.dto.response.PaymentPreparationResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentServiceSubscriptionRepository paymentServiceSubscriptionRepository;
    private final PaymentServiceOrderRepository paymentServiceOrderRepository;
    private final PaymentProcessor paymentProcessor;
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
        paymentProcessor.processClientVerification(request.paymentId(), request.iamPortTransactionId(), memberId);
    }

    // 웹 훅 용입니다.
    @Transactional
    public void verifyAndProcessWebhook(String requestBody) throws IOException {
        paymentProcessor.processWebhook(requestBody, objectMapper);
    }

    @Transactional
    public void cancelSubscription(UUID memberId, CancelSubscriptionRequest request) {
        Subscription subscription = paymentServiceSubscriptionRepository.findByMemberId(memberId);

        Order order = paymentServiceOrderRepository.findBySubscriptionId(subscription.getId());

        order.cancel(); // 여기서 더티체킹
        subscription.deactivate(); // 여기서 더티체킹
    }
}
