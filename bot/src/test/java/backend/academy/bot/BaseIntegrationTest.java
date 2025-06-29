package backend.academy.bot;

import java.util.UUID;
import org.junit.jupiter.api.TestInstance;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.kafka.KafkaContainer;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseIntegrationTest {

    protected static final GenericContainer<?> redis;
    protected static final KafkaContainer kafka;

    static {
        redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);
        kafka = new KafkaContainer("apache/kafka-native:3.8.1").withExposedPorts(9092);
        redis.start();
        kafka.start();
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add(
                "app.update-events.consumer-group-id", () -> UUID.randomUUID().toString());
    }
}
