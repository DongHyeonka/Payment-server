package com.synapse.payment_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.synapse.payment_service.domain.Subscription;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

}
