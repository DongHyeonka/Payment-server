package com.synapse.payment_service.domain.repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.synapse.payment_service.domain.entity.Subscription;
import com.synapse.payment_service.domain.enums.SubscriptionStatus;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    Optional<Subscription> findByMemberId(UUID memberId); // 여기도 동일하게 인덱스 걸려있음

    Optional<Subscription> findByBillingKey(String billingKey); // 여기도 동일하게 걸려있음

    @Query("SELECT s FROM Subscription s WHERE s.status = 'ACTIVE' AND s.autoRenew = true AND s.billingKey IS NOT NULL AND s.expiresAt >= :startOfDay AND s.expiresAt < :endOfDay")
    Slice<Subscription> findActiveSubscriptionsDueForRenewal(
        @Param("startOfDay") ZonedDateTime startOfDay, 
        @Param("endOfDay") ZonedDateTime endOfDay, 
        Pageable pageable
    );

    // 복합 인덱스를 걸어서 성능 최적화 진행 끝
    @Query("SELECT s FROM Subscription s WHERE s.status IN (:statuses) AND s.expiresAt < :currentTime")
    Slice<Subscription> findExpiredSubscriptionsWithCursor(
        @Param("statuses") List<SubscriptionStatus> statuses, 
        @Param("currentTime") ZonedDateTime currentTime, 
        Pageable pageable
    );
}
