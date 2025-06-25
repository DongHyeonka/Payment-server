package com.synapse.payment_service.service.converter.impl;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.payment_service.domain.Order;
import com.synapse.payment_service.domain.enums.PaymentStatus;
import com.synapse.payment_service.dto.PaymentData;
import com.synapse.payment_service.service.converter.PaymentStatusConverter;

import io.portone.sdk.server.payment.PayPendingPayment;
import io.portone.sdk.server.payment.Payment;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class PayPendingPaymentConverter implements PaymentStatusConverter {

    @Override
    public boolean canHandle(Class<? extends Payment> paymentStatus) {
        return paymentStatus.equals(PayPendingPayment.class);
    }

    @Override
    @Transactional
    public void processPayment(Order order, Payment payment) {
        log.info("결제 완료 대기 처리 시작. merchantUid={}", order.getMerchantUid());
        
        // 주문 상태를 결제 완료 대기로 업데이트
        order.updateStatus(PaymentStatus.PAY_PENDING);
        
        log.info("결제 완료 대기 처리 완료. merchantUid={}", order.getMerchantUid());
    }
} 