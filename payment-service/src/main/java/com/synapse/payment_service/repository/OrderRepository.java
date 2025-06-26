package com.synapse.payment_service.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.synapse.payment_service.domain.Order;
import com.synapse.payment_service.domain.Subscription;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByPaymentId(String paymentId);
    Optional<Order> findBySubscription(Subscription subscription);
}
