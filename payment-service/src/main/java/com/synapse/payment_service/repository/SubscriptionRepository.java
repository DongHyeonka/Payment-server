package com.synapse.payment_service.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.synapse.payment_service.domain.Subscription;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findByMemberId(UUID memberId);
    Optional<Subscription> findByBillingKey(String billingKey);
    
    @Query("SELECT s FROM Subscription s WHERE s.status = 'ACTIVE' AND s.autoRenew = true AND s.billingKey IS NOT NULL AND DATE(s.expiresAt) <= :renewalDate")
    List<Subscription> findActiveSubscriptionsDueForRenewal(@Param("renewalDate") LocalDate renewalDate);
}
