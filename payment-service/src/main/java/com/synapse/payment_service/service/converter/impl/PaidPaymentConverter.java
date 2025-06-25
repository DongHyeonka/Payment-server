package com.synapse.payment_service.service.converter.impl;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.payment_service.domain.Order;
import com.synapse.payment_service.domain.Subscription;
import com.synapse.payment_service.domain.enums.PaymentStatus;
import com.synapse.payment_service.dto.PaymentData;
import com.synapse.payment_service.service.converter.PaymentStatusConverter;

import io.portone.sdk.server.payment.PaidPayment;
import io.portone.sdk.server.payment.Payment;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class PaidPaymentConverter implements PaymentStatusConverter {

    @Override
    public boolean canHandle(Class<? extends Payment> paymentStatus) {
        return paymentStatus.equals(PaidPayment.class);
    }

    @Override
    @Transactional
    public void processPayment(Order order, Payment payment) {
        if (!(payment instanceof Payment.Recognized recognizedPayment)) {
            throw new RuntimeException("결제 정보를 인식할 수 없습니다");
        }
        log.info("결제 완료 처리 시작. merchantUid={}, amount={}", 
                order.getMerchantUid(), recognizedPayment.getAmount().getTotal());
        
        // 주문 상태 업데이트
        order.updateStatus(PaymentStatus.PAID);
        
        // 구독 상태 활성화
        Subscription subscription = order.getSubscription();
        subscription.activate();
        
        log.info("결제 완료 처리 완료. merchantUid={}", order.getMerchantUid());
    }
} 