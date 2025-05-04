package backend.academy.scrapper;

import static backend.academy.scrapper.TestcontainersConfiguration.migrateDatabase;

import org.junit.jupiter.api.TestInstance;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseIntegrationTest {

    protected static final GenericContainer<?> redis;
    protected static final KafkaContainer kafka;
    protected static final PostgreSQLContainer<?> postgres;

    static {
        redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);
        kafka = new KafkaContainer("apache/kafka-native:3.8.1").withExposedPorts(9092);
        postgres = new PostgreSQLContainer<>("postgres:17-alpine")
                .withExposedPorts(5432)
                .withDatabaseName("local")
                .withUsername("postgres")
                .withPassword("test");
        redis.start();
        kafka.start();
        postgres.start();
        migrateDatabase(postgres);
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
