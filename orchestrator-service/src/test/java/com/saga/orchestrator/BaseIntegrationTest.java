package com.saga.orchestrator;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.Redisson;

@SpringBootTest
@Testcontainers
@Import(BaseIntegrationTest.RedisTestConfig.class)
public abstract class BaseIntegrationTest {

    @Container
    @org.springframework.boot.testcontainers.service.connection.ServiceConnection
    public static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("orchestrator_test_db")
            .withUsername("testuser")
            .withPassword("testpass")
            .withInitScript("init-schema.sql");

    @Container
    @org.springframework.boot.testcontainers.service.connection.ServiceConnection
    public static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    @Container
    @org.springframework.boot.testcontainers.service.connection.ServiceConnection
    public static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Automatically handled by @ServiceConnection
    }

    @org.springframework.boot.test.context.TestConfiguration
    public static class RedisTestConfig {
        @Bean
        public RedissonClient redissonClient() {
            Config config = new Config();
            config.useSingleServer()
                  .setAddress("redis://" + redis.getHost() + ":" + redis.getMappedPort(6379));
            return Redisson.create(config);
        }
    }
}
