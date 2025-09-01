package com.synapse.payment_service.service.eventuate;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.payment_service.domain.entity.Member;
import com.synapse.payment_service.domain.entity.Subscription;
import com.synapse.payment_service.domain.enums.SubscriptionTier;
import com.synapse.payment_service.domain.repository.MemberRepository;
import com.synapse.payment_service.domain.repository.SubscriptionRepository;
import com.synapse.payment_service.exception.ExceptionCode;
import com.synapse.payment_service.exception.NotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SubscriptionService {
    private final SubscriptionRepository subscriptionRepository;
    private final MemberRepository memberRepository;

    @Transactional(propagation = Propagation.MANDATORY) // 상위 트랜잭션이 있을 때만 동작이 되어야 한다.
    public Subscription createdSubscription(
        UUID subscriptionId, UUID userId, String subscriptionTier, String nextRenewalDate
    ) {
        if (subscriptionRepository.existsById(subscriptionId)) {
            return subscriptionRepository.findById(subscriptionId).get();
        }

        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ExceptionCode.MEMBER_NOT_FOUND)); // 1대 1 연관 관계가 걸려 있기에 확인 먼저 필요 외래키가 없는데 데이터를 집어넣을 수도 있음

        Subscription subscription = Subscription.builder()
                .id(subscriptionId)
                .member(member)
                .tier(SubscriptionTier.fromTier(subscriptionTier))
                .remainingChatCredits(SubscriptionTier.FREE.getMaxRequestCount())
                .expiresAt(ZonedDateTime.parse(nextRenewalDate))
                .build();

        subscriptionRepository.save(subscription);
        return subscription;
    }
}
