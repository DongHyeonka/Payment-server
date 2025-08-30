package com.synapse.payment_service.service.externalservice;

import org.springframework.stereotype.Service;

import com.synapse.payment_service.configuration.PortOneClientProperties;
import com.synapse.payment_service.domain.entity.Order;

import io.portone.sdk.server.PortOneClient;
import io.portone.sdk.server.common.PaymentAmountInput;
import io.portone.sdk.server.payment.PayWithBillingKeyResponse;
import io.portone.sdk.server.payment.Payment;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PortOneService {
    private final PortOneClient portOneClient;
    private final PortOneClientProperties portOneClientProperties;

    // 빌링키를 사용한 결제
    // 여기서 서킷 브레이커를 걸 예정
    public PayWithBillingKeyResponse payWithBillingKey(Order order, String billingKey, PaymentAmountInput amount) {
        return portOneClient.getPayment().payWithBillingKey(
                order.getPaymentId(),
                billingKey,
                portOneClientProperties.channelKey(),
                order.getOrderName(),
                null, null, amount, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null).join();
    }

    // 일반 결제
    // 여기서 서킷 브레이커를 걸 예정
    public Payment payment(String iamPortTransactionId) {
        return portOneClient.getPayment().getPayment(iamPortTransactionId).join();
    }
}
