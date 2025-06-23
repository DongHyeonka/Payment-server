package com.synapse.payment_service.domain;

import java.time.ZonedDateTime;
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

    @Builder
    public Subscription(UUID memberId, SubscriptionTier tier, int remainingChatCredits, ZonedDateTime expiresAt, SubscriptionStatus status) {
        this.memberId = memberId;
        this.tier = tier;
        this.remainingChatCredits = remainingChatCredits;
        this.expiresAt = expiresAt;
        this.status = status;
    }
}
