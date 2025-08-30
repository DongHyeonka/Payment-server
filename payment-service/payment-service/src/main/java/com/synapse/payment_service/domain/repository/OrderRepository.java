package com.synapse.payment_service.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.synapse.payment_service.domain.entity.Order;
import com.synapse.payment_service.domain.enums.PaymentStatus;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByPaymentId(String paymentId); // 유니크로 걸려 있기에 인덱스 생성되어 있음

    Optional<Order> findBySubscriptionId(UUID subscriptionId); // 유니크로 걸려 있기에 인덱스 자동으로 생성되어 있음

    boolean existsBySubscriptionIdAndPaymentStatus(UUID subscriptionId, PaymentStatus paymentStatus);
}
