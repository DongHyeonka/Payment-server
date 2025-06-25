package com.synapse.payment_service.service.converter.impl;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.payment_service.domain.Order;
import com.synapse.payment_service.domain.enums.PaymentStatus;
import com.synapse.payment_service.service.converter.PaymentStatusConverter;

import io.portone.sdk.server.payment.FailedPayment;
import io.portone.sdk.server.payment.Payment;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class FailedPaymentConverter implements PaymentStatusConverter {

    @Override
    public boolean canHandle(Class<? extends Payment> paymentStatus) {
        return paymentStatus.equals(FailedPayment.class);
    }

    @Override
    @Transactional
    public void processPayment(Order order, Payment payment) {
        // 주문 상태를 실패로 업데이트
        order.updateStatus(PaymentStatus.FAILED);
    }
} 