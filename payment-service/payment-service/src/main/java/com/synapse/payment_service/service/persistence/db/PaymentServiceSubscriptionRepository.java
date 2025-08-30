package com.synapse.payment_service.service.persistence.db;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.payment_service.domain.entity.Subscription;
import com.synapse.payment_service.domain.enums.SubscriptionStatus;
import com.synapse.payment_service.domain.repository.SubscriptionRepository;
import com.synapse.payment_service.exception.ExceptionCode;
import com.synapse.payment_service.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentServiceSubscriptionRepository {
    private final SubscriptionRepository subscriptionRepository;

    // 캐시를 적용할 수 있음
    public Subscription findByMemberId(UUID memberId) {
        return subscriptionRepository.findByMemberId(memberId)
            .orElseThrow(() -> new NotFoundException(ExceptionCode.SUBSCRIPTION_NOT_FOUND));
    }

    // 캐시를 적용할 수 있음
    public Slice<Subscription> findExpiredSubscriptionsWithCursor(Pageable pageable) {
        ZonedDateTime currentTime = ZonedDateTime.now();

        return subscriptionRepository.findExpiredSubscriptionsWithCursor(
                List.of(SubscriptionStatus.CANCELED, SubscriptionStatus.PAYMENT_FAILED),
                currentTime, 
                pageable);
    }

    // 캐시를 적용할 수 있음
    public Slice<Subscription> findActiveSubscriptionsDueForRenewal(ZonedDateTime startOfDay, ZonedDateTime endOfDay, Pageable pageable) {
        return subscriptionRepository.findActiveSubscriptionsDueForRenewal(
            startOfDay, endOfDay, pageable
        );
    }

    @Transactional
    public void save(Subscription subscription) {
        subscriptionRepository.save(subscription);
    }
}
