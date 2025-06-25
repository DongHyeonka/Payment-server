package com.synapse.payment_service.service.converter;

import com.synapse.payment_service.domain.Order;

import io.portone.sdk.server.payment.Payment;

/**
 * 결제 상태별 처리를 담당하는 컨버터 인터페이스
 */
public interface PaymentStatusConverter {
    
    /**
     * 해당 컨버터가 주어진 결제 상태를 처리할 수 있는지 확인
     */
    boolean canHandle(Class<? extends Payment> paymentStatus);
    
    /**
     * 결제 상태에 따른 주문 처리 로직 실행
     */
    void processPayment(Order order, Payment payment);
} 