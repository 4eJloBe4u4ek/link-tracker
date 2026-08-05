package backend.academy.scrapper.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
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
class PuppetTheatreSnapshotRepositoryIntegrationTest {
    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    private LettuceConnectionFactory connectionFactory;
    private StringRedisTemplate redisTemplate;
    private PuppetTheatreSnapshotRepository repository;

    @BeforeEach
    void setUp() {
        RedisStandaloneConfiguration configuration =
                new RedisStandaloneConfiguration(REDIS.getHost(), REDIS.getMappedPort(6379));
        connectionFactory = new LettuceConnectionFactory(configuration);
        connectionFactory.afterPropertiesSet();
        redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
        repository = new PuppetTheatreSnapshotRepository(redisTemplate);
    }

    @AfterEach
    void tearDown() {
        connectionFactory.destroy();
    }

    @Test
    void shouldStoreAndReplaceSnapshotsAsSingleStringValues() {
        // Arrange & Assert
        assertThat(repository.getAvailableSessionKeys(42L)).isEqualTo(Optional.empty());

        // Act
        repository.replaceAvailableSessionKeys(42L, Set.of());

        // Assert
        assertThat(repository.getAvailableSessionKeys(42L)).contains(Set.of());

        Long ttl = redisTemplate.getExpire("puppet-theatre:42:snapshot");
        assertThat(ttl).isGreaterThan(0).isLessThanOrEqualTo(Duration.ofDays(90).toSeconds());

        // Act
        repository.replaceAvailableSessionKeys(42L, Set.of("session-1", "session-2"));

        // Assert
        assertThat(repository.getAvailableSessionKeys(42L)).contains(Set.of("session-1", "session-2"));

        // Act
        repository.replaceAvailableSessionKeys(42L, Set.of("session-3"));

        // Assert
        assertThat(repository.getAvailableSessionKeys(42L)).contains(Set.of("session-3"));
    }
}
