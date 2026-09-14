package backend.academy.scrapper.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
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
class TicketproSnapshotRepositoryTest {
    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private TicketproSnapshotRepository repository;

    @BeforeEach
    void setUp() {
        repository = new TicketproSnapshotRepository(redisTemplate);
    }

    @Test
    void shouldStoreSnapshotUsingTicketproKeyNewlinesAndNinetyDayTtl() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        Set<String> eventKeys = new LinkedHashSet<>(List.of("event-1", "event-2"));

        // Act
        repository.replaceAvailableEventKeys(42L, eventKeys);

        // Assert
        verify(valueOperations).set("ticketpro:42:snapshot", "event-1\nevent-2", Duration.ofDays(90));
    }

    @Test
    void shouldReadSnapshotAsImmutableSet() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("ticketpro:42:snapshot")).thenReturn("event-1\nevent-2");

        // Act
        Optional<Set<String>> snapshot = repository.getAvailableEventKeys(42L);

        // Assert
        assertThat(snapshot).contains(Set.of("event-1", "event-2"));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.orElseThrow()
                .add("event-3"));
    }

    @Test
    void shouldDistinguishExplicitEmptySnapshotFromMissingSnapshot() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("ticketpro:42:snapshot")).thenReturn("");

        // Act
        Optional<Set<String>> snapshot = repository.getAvailableEventKeys(42L);

        // Assert
        assertThat(snapshot).contains(Set.of());
    }

    @Test
    void shouldPropagateReplacementFailureAfterAttemptingAtomicSet() {
        // Arrange
        String snapshotKey = "ticketpro:42:snapshot";
        RedisSystemException failure = new RedisSystemException("write failed", new IllegalStateException("redis"));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(failure).when(valueOperations).set(eq(snapshotKey), eq("new-event"), eq(Duration.ofDays(90)));

        // Act
        RedisSystemException thrown = assertThrows(
                RedisSystemException.class, () -> repository.replaceAvailableEventKeys(42L, Set.of("new-event")));

        // Assert
        assertThat(thrown).isSameAs(failure);
        verify(valueOperations).set(snapshotKey, "new-event", Duration.ofDays(90));
    }
}
