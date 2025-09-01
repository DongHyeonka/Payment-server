package com.synapse.payment_service.eventuate.configuration;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * repository에 관한 의존성 설정
 */
@Configuration
@EnableJpaRepositories
@EnableAutoConfiguration
@Import({ EventuateConfig.class })
public class PaymentServiceWithRepositoriesConfiguration {
    
}
