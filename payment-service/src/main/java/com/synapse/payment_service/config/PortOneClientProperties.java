package com.synapse.payment_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "iamport")
public record PortOneClientProperties(
    String apiSecret,
    String baseUrl,
    String midKey
) {
    
}
