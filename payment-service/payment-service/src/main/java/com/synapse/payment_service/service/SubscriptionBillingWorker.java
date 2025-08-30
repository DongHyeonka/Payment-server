package com.synapse.payment_service.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.payment_service.domain.entity.Order;
import com.synapse.payment_service.domain.entity.Subscription;
import com.synapse.payment_service.domain.enums.PaymentStatus;
import com.synapse.payment_service.service.externalservice.PortOneService;
import com.synapse.payment_service.service.persistence.db.PaymentServiceOrderRepository;
import com.synapse.payment_service.service.persistence.db.PaymentServiceSubscriptionRepository;

import io.portone.sdk.server.common.PaymentAmountInput;
import io.portone.sdk.server.payment.PayWithBillingKeyResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionBillingWorker {
    private final PortOneService portOneService;
    private final PaymentServiceOrderRepository paymentServiceOrderRepository;
    private final PaymentServiceSubscriptionRepository paymentServiceSubscriptionRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW) // 결제와 같은 오래걸리는 부분에 대한 트랜잭션 분리
    public void chargeAndRenewSubscription(Subscription subscription) {
        if (paymentServiceOrderRepository.existsPendingOrderFor(subscription.getId())) {
            log.warn("이미 처리 중인 주문이 있어 이번 결제 시도를 건너뜁니다. subscriptionId={}", subscription.getId());
            return;
        }

        Order order = Order.createForSubscription(subscription, subscription.getTier());
        paymentServiceOrderRepository.save(order);

        String billingKey = subscription.getBillingKey();
        PaymentAmountInput amount = new PaymentAmountInput(subscription.getTier().getMonthlyPrice().longValue(), 0L, 0L);

        // 빌링키 결제 요청
        try {
            PayWithBillingKeyResponse response = portOneService.payWithBillingKey(order, billingKey, amount);
            successHandler(response, order, subscription);
            log.info("구독 결제 성공. paymentId={}, orderName={}, subscriptionId={}", order.getPaymentId(), order.getOrderName(), subscription.getId());
        } catch (Exception e) {
            failureHandler(order, subscription);
            log.error("구독 결제 실패. paymentId={}, orderName={}, subscriptionId={}", order.getPaymentId(), order.getOrderName(), subscription.getId());
        }
    }

    private void successHandler(PayWithBillingKeyResponse response, Order order, Subscription subscription) {
        // 기존 Order 객체 업데이트
        order.updateIamPortTransactionId(response.getPayment().getPgTxId());
        order.markAsPaid();

        subscription.renewSubscription(subscription.getTier());

        paymentServiceOrderRepository.save(order);
    }

    private void failureHandler(Order order, Subscription subscription) {
        // 기존 Order 객체 상태를 FAILED로 업데이트
        order.updateStatus(PaymentStatus.FAILED);
        paymentServiceOrderRepository.save(order);

        // 구독 상태를 PAYMENT_FAILED로 변경하고 retryCount 증가
        subscription.handlePaymentFailure();
        paymentServiceSubscriptionRepository.save(subscription);
    }
}
