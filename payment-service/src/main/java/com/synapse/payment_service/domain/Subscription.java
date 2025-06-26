package com.synapse.payment_service.domain;

import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

import com.synapse.payment_service.common.BaseEntity;
import com.synapse.payment_service.domain.enums.SubscriptionStatus;
import com.synapse.payment_service.domain.enums.SubscriptionTier;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "subscriptions")
public class Subscription extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // account-service의 Member와 1:1로 매핑되는 고유 식별자
    @Column(nullable = false, unique = true, columnDefinition = "uuid")
    private UUID memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionTier tier;

    @Column(nullable = false)
    private int remainingChatCredits;

    private ZonedDateTime expiresAt;

    private String billingKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status;

    @Column(nullable = false)
    private boolean autoRenew = true;

    @Builder
    public Subscription(UUID memberId, SubscriptionTier tier, int remainingChatCredits, ZonedDateTime expiresAt, SubscriptionStatus status) {
        this.memberId = memberId;
        this.tier = tier;
        this.remainingChatCredits = remainingChatCredits;
        this.expiresAt = expiresAt;
        this.status = status;
        this.autoRenew = true;
    }

    public void deactivate() {
        this.status = SubscriptionStatus.CANCELED;
        this.billingKey = null;
        this.autoRenew = false;
    }

    public void updateBillingKey(String billingKey) {
        this.billingKey = billingKey;
        this.remainingChatCredits = this.tier.getMaxRequestCount();
    }
    
    public void renewSubscription(SubscriptionTier newTier) {
        // 기존 구독 갱신 - 기존 만료일에서 1달 연장
        ZonedDateTime currentExpiresAt = this.expiresAt != null ? this.expiresAt : ZonedDateTime.now();
        
        // 현재 만료일이 해당 월의 마지막 날인지 확인
        boolean isLastDayOfMonth = currentExpiresAt.getDayOfMonth() == currentExpiresAt.toLocalDate().lengthOfMonth();

        if (isLastDayOfMonth) {
            // 만약 마지막 날이었다면, 다음 달의 마지막 날로 설정
            this.expiresAt = currentExpiresAt.plusMonths(1).with(TemporalAdjusters.lastDayOfMonth());
        } else {
            // 그렇지 않다면, 단순히 한 달을 더 한다 (예: 15일 -> 다음 달 15일)
            this.expiresAt = currentExpiresAt.plusMonths(1);
        }
        
        // 갱신 시 크레딧도 해당 티어의 기본 크레딧으로 초기화
        this.remainingChatCredits = this.tier.getMaxRequestCount();
        this.status = SubscriptionStatus.ACTIVE;
        this.tier = newTier;
        this.autoRenew = true;
    }
}
