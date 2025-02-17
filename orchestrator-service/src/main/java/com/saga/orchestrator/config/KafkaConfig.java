package com.saga.orchestrator.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic sagaEventsTopic() {
        return TopicBuilder.name("saga-events").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic flightCommandsTopic() {
        return TopicBuilder.name("flight-commands").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic hotelCommandsTopic() {
        return TopicBuilder.name("hotel-commands").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic carCommandsTopic() {
        return TopicBuilder.name("car-commands").partitions(3).replicas(1).build();
    }
}
