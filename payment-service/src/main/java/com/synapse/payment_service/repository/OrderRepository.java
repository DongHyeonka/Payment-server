package com.synapse.payment_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.synapse.payment_service.domain.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {

}
