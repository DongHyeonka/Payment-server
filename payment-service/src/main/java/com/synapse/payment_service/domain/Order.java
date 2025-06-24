package com.synapse.payment_service.domain;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import com.synapse.payment_service.common.BaseEntity;
import com.synapse.payment_service.domain.enums.PaymentStatus;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "orders")
public class Order extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(unique = true)
    private String iamportUid; // 아임포트에서 사용하는 결제 건별 고유 ID, 환불시 사용

    @Column(nullable = false, unique = true)
    private String merchantUid; // 주문별 고유 ID. 중복 결제 방지

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    private ZonedDateTime paidAt;

    @Builder
    public Order(Subscription subscription, String iamportUid, String merchantUid, BigDecimal amount, PaymentStatus status, ZonedDateTime paidAt) {
        this.subscription = subscription;
        this.iamportUid = iamportUid;
        this.merchantUid = merchantUid;
        this.amount = amount;
        this.status = status;
        this.paidAt = paidAt;
    }
}
