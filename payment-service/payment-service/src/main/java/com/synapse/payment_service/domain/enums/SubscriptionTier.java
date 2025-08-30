package com.synapse.payment_service.domain.enums;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SubscriptionTier {
    PRO("pro", "subscription-pro", 0, 100, new BigDecimal("100000")),
    FREE("free", "subscription-free", 0, 10, BigDecimal.ZERO),
    UNKNOWN("unknown", "subscription-free", 0, 10, BigDecimal.ZERO);

    private final String subscriptionTierName;
    private final String policyName;
    private final int currentSubscriptionCount;
    private final int maxRequestCount;
    private final BigDecimal monthlyPrice;

    public static final Map<String, SubscriptionTier> TIER_MAP = Collections.unmodifiableMap(
        Stream.of(values())
            .collect(Collectors.toMap(SubscriptionTier::getSubscriptionTierName, Function.identity()))
    );

    public static SubscriptionTier fromTier(String tierName) {
        SubscriptionTier result = TIER_MAP.get(tierName);
        if (result == null) {
            throw new IllegalArgumentException("일치하는 티어 타입이 없습니다." + tierName);
        }
        return result;
    }
}
