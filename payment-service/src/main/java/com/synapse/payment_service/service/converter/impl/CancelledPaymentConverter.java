package com.synapse.payment_service.service.converter.impl;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.payment_service.domain.Order;
import com.synapse.payment_service.domain.enums.PaymentStatus;
import com.synapse.payment_service.service.converter.PaymentStatusConverter;

import io.portone.sdk.server.payment.CancelledPayment;
import io.portone.sdk.server.payment.Payment;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CancelledPaymentConverter implements PaymentStatusConverter {

    @Override
    public boolean canHandle(Class<? extends Payment> paymentStatus) {
        return paymentStatus.equals(CancelledPayment.class);
    }

    @Override
    @Transactional
    public void processPayment(Order order, Payment payment) {
        log.info("결제 취소 처리 시작. merchantUid={}", order.getMerchantUid());
        
        // 주문 상태를 취소로 업데이트
        order.updateStatus(PaymentStatus.CANCELLED);
        
        // 구독 비활성화
        order.getSubscription().deactivate();
        
        log.info("결제 취소 처리 완료. merchantUid={}", order.getMerchantUid());
    }
} 