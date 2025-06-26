package com.synapse.payment_service.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.payment_service.config.PortOneClientProperties;
import com.synapse.payment_service.domain.Order;
import com.synapse.payment_service.domain.Subscription;
import com.synapse.payment_service.domain.enums.PaymentStatus;
import com.synapse.payment_service.domain.enums.SubscriptionTier;
import com.synapse.payment_service.repository.OrderRepository;
import com.synapse.payment_service.repository.SubscriptionRepository;

import io.portone.sdk.server.PortOneClient;
import io.portone.sdk.server.common.PaymentAmountInput;
import io.portone.sdk.server.payment.PayWithBillingKeyResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionBillingService {
    private final SubscriptionRepository subscriptionRepository;
    private final OrderRepository orderRepository;
    private final PortOneClient portOneClient;
    private final PortOneClientProperties portOneClientProperties;

    @Transactional
    public void processDailySubscriptions() {
        // 오늘이 다음 결제일인 모든 활성 구독을 찾는다.
        List<Subscription> targets = subscriptionRepository.findActiveSubscriptionsDueForRenewal(LocalDate.now());
        for (Subscription subscription : targets) {
            chargeWithBillingKey(subscription);
        }
    }

    private void chargeWithBillingKey(Subscription subscription) {
        SubscriptionTier tier = subscription.getTier();
        String billingKey = subscription.getBillingKey();
        PaymentAmountInput amount = new PaymentAmountInput(tier.getMonthlyPrice().longValue(), 0L, 0L);
        String orderName = tier.getTierName() + "_" + "subscription";
        String paymentId = orderName + "_" + UUID.randomUUID();

        // 빌링키 결제 요청
        try {
            PayWithBillingKeyResponse response = portOneClient.getPayment().payWithBillingKey(paymentId, billingKey, portOneClientProperties.channelKey(), orderName, null, null, amount, null, null, null, null, null, null, null, null, null, null, null, null, null, null).join();
            successHandler(response, paymentId, orderName, subscription);
            log.info("구독 결제 성공. paymentId={}, orderName={}, subscriptionId={}", paymentId, orderName, subscription.getId());
        } catch (Exception e) {
            failureHandler(paymentId, orderName, subscription);
            log.error("구독 결제 실패. paymentId={}, orderName={}, subscriptionId={}", paymentId, orderName, subscription.getId());
        }
    }

    private void successHandler(PayWithBillingKeyResponse response, String paymentId, String orderName, Subscription subscription) {
        // 결제 정보 저장
        Order order = Order.builder()
                .subscription(subscription)
                .iamPortTransactionId(response.getPayment().getPgTxId())
                .paymentId(paymentId)
                .amount(subscription.getTier().getMonthlyPrice())
                .status(PaymentStatus.PAID)
                .paidAt(response.getPayment().getPaidAt().atZone(ZoneId.of("Asia/Seoul")))
                .build();
                
        subscription.renewSubscription(subscription.getTier());

        orderRepository.save(order);
    }

    private void failureHandler(String paymentId, String orderName, Subscription subscription) {
        // 결제 실패 정보 저장
        Order order = Order.builder()
                .subscription(subscription)
                .paymentId(paymentId)
                .amount(subscription.getTier().getMonthlyPrice())
                .status(PaymentStatus.FAILED)
                .build();
        orderRepository.save(order);
    }
}
