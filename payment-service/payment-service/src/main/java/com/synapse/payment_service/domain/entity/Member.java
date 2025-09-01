package com.synapse.payment_service.domain.entity;

import java.util.UUID;

import com.synapse.payment_service.domain.common.BaseTimeEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "members")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {
    @Id
    @Column(name = "member_id", nullable = false, unique = true, columnDefinition = "uuid")
    private UUID memberId;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "user_name", nullable = false)
    private String userName;

    @OneToOne(mappedBy = "member", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Subscription subscription;

    @Builder
    public Member(UUID memberId, String email, String userName) {
        this.memberId = memberId;
        this.email = email;
        this.userName = userName;
    }
}
