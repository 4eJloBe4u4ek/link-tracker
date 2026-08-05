package backend.academy.scrapper.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class PuppetTheatreSnapshotRepositoryTest {
    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private PuppetTheatreSnapshotRepository repository;

    @BeforeEach
    void setUp() {
        repository = new PuppetTheatreSnapshotRepository(redisTemplate);
    }

    @Test
    void shouldKeepPreviousSnapshotWhenReplacementFails() {
        // Arrange
        String snapshotKey = "puppet-theatre:42:snapshot";
        RedisSystemException failure = new RedisSystemException("write failed", new IllegalStateException("redis"));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(snapshotKey)).thenReturn("old-session");
        doThrow(failure).when(valueOperations).set(eq(snapshotKey), anyString(), eq(Duration.ofDays(90)));

        // Act & Assert
        assertThrows(
                RedisSystemException.class, () -> repository.replaceAvailableSessionKeys(42L, Set.of("new-session")));

        // Assert
        assertThat(repository.getAvailableSessionKeys(42L)).contains(Set.of("old-session"));
    }
}
