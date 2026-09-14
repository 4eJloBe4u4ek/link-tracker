package backend.academy.scrapper.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class TicketproSnapshotRepositoryIntegrationTest {
    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    private LettuceConnectionFactory connectionFactory;
    private StringRedisTemplate redisTemplate;
    private TicketproSnapshotRepository repository;

    @BeforeEach
    void setUp() {
        RedisStandaloneConfiguration configuration =
                new RedisStandaloneConfiguration(REDIS.getHost(), REDIS.getMappedPort(6379));
        connectionFactory = new LettuceConnectionFactory(configuration);
        connectionFactory.afterPropertiesSet();
        redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
        repository = new TicketproSnapshotRepository(redisTemplate);
    }

    @AfterEach
    void tearDown() {
        connectionFactory.destroy();
    }

    @Test
    void shouldStoreAndReplaceSnapshotsAsSingleStringValues() {
        // Arrange
        String snapshotKey = "ticketpro:42:snapshot";

        // Act
        Optional<Set<String>> missingSnapshot = repository.getAvailableEventKeys(42L);

        // Assert
        assertThat(missingSnapshot).isEqualTo(Optional.empty());

        // Act
        repository.replaceAvailableEventKeys(42L, Set.of());

        // Assert
        assertThat(repository.getAvailableEventKeys(42L)).contains(Set.of());
        assertThat(redisTemplate.hasKey(snapshotKey)).isTrue();
        assertThat(redisTemplate.opsForValue().get(snapshotKey)).isEmpty();
        Long ttl = redisTemplate.getExpire(snapshotKey);
        assertThat(ttl)
                .isBetween(Duration.ofDays(90).minusSeconds(5).toSeconds(), Duration.ofDays(90).toSeconds());

        // Act
        repository.replaceAvailableEventKeys(42L, new LinkedHashSet<>(List.of("event-1", "event-2")));

        // Assert
        assertThat(repository.getAvailableEventKeys(42L)).contains(Set.of("event-1", "event-2"));
        assertThat(redisTemplate.opsForValue().get(snapshotKey)).isEqualTo("event-1\nevent-2");

        // Act
        repository.replaceAvailableEventKeys(42L, Set.of("event-3"));

        // Assert
        assertThat(repository.getAvailableEventKeys(42L)).contains(Set.of("event-3"));
    }
}
