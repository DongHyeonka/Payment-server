package com.synapse.payment_service.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.portone.sdk.server.PortOneClient;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableConfigurationProperties(PortOneClientProperties.class)
@RequiredArgsConstructor
public class PortOneClientConfig {
    private final PortOneClientProperties properties;

    @Bean
    public PortOneClient portOneClient() {
        // PortOneClient는 스레드에 안전하며 애플리케이션 전반에 걸쳐 재사용 가능합니다.
        return new PortOneClient(properties.apiSecret(), properties.baseUrl(), properties.midKey());
    }
}
