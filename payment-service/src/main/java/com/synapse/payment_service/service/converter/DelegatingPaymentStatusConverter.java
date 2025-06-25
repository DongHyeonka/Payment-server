package com.synapse.payment_service.service.converter;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.synapse.payment_service.domain.Order;

import io.portone.sdk.server.payment.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DelegatingPaymentStatusConverter implements PaymentStatusConverter {

    private final List<PaymentStatusConverter> converters;

    @Override
    public boolean canHandle(Class<? extends Payment> paymentStatus) {
        return converters.stream()
                .anyMatch(converter -> converter.canHandle(paymentStatus));
    }

    @Override
    public void processPayment(Order order, Payment payment) {
        Assert.notNull(order, "order cannot be null");
        Assert.notNull(payment, "payment cannot be null");

        for (PaymentStatusConverter converter : this.converters) {
            if (converter.canHandle(payment.getClass())) {
                log.info("결제 상태 처리: {} -> {}", payment.getClass(), converter.getClass().getSimpleName());
                converter.processPayment(order, payment);
                return;
            }
        }
        
        // 처리할 수 있는 컨버터가 없는 경우
        log.error("지원하지 않는 결제 상태: {}", payment.getClass());
        throw new IllegalArgumentException("지원하지 않는 결제 상태: " + payment.getClass());
    }
} 