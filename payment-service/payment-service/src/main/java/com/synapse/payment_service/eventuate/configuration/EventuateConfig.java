package com.synapse.payment_service.eventuate.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import io.eventuate.tram.spring.events.publisher.TramEventsPublisherConfiguration;
import io.eventuate.tram.spring.jdbckafka.TramJdbcKafkaConfiguration;

/**
 * publisher에 관한 의존성 설정
 */
@Configuration
@Import({ TramEventsPublisherConfiguration.class, TramJdbcKafkaConfiguration.class })
public class EventuateConfig {
    
}
