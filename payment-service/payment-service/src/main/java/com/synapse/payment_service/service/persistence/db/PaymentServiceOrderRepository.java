package com.synapse.payment_service.service.persistence.db;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.payment_service.domain.entity.Order;
import com.synapse.payment_service.domain.enums.PaymentStatus;
import com.synapse.payment_service.domain.repository.OrderRepository;
import com.synapse.payment_service.exception.ExceptionCode;
import com.synapse.payment_service.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentServiceOrderRepository {
    private final OrderRepository orderRepository;

    // 캐시를 적용할 수 있음
    public Order findByOrderId(String paymentId) {
        return orderRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new NotFoundException(ExceptionCode.ORDER_NOT_FOUND));
    }

    // 캐시를 적용할 수 있음
    public Order findBySubscriptionId(UUID subscriptionId) {
        return orderRepository.findBySubscriptionId(subscriptionId)
                .orElseThrow(() -> new NotFoundException(ExceptionCode.ORDER_NOT_FOUND));
    }

    // 캐시를 적용할 수 있음
    public boolean existsPendingOrderFor(UUID subscriptionId) {
        return orderRepository.existsBySubscriptionIdAndPaymentStatus(subscriptionId, PaymentStatus.PENDING);
    }

    @Transactional
    public Order save(Order order) {
        return orderRepository.save(order);
    }
}
