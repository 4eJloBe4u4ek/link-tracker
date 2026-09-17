package backend.academy.scrapper.repository;

import static org.assertj.core.api.Assertions.assertThat;
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
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void shouldReuseTheExistingPuppetTheatreSnapshotKeyAndTtl() {
        Set<String> keys = new LinkedHashSet<>(List.of("first", "second"));

        repository.replaceAvailableSessionKeys(42L, keys);

        verify(valueOperations).set("puppet-theatre:42:snapshot", "first\nsecond", Duration.ofDays(90));
    }

    @Test
    void shouldDistinguishMissingEmptyAndStoredSnapshots() {
        when(valueOperations.get("puppet-theatre:1:snapshot")).thenReturn(null);
        when(valueOperations.get("puppet-theatre:2:snapshot")).thenReturn("");
        when(valueOperations.get("puppet-theatre:3:snapshot")).thenReturn("first\nsecond");

        Optional<Set<String>> missing = repository.getAvailableSessionKeys(1L);
        Optional<Set<String>> empty = repository.getAvailableSessionKeys(2L);
        Optional<Set<String>> stored = repository.getAvailableSessionKeys(3L);

        assertThat(missing).isEmpty();
        assertThat(empty).contains(Set.of());
        assertThat(stored).contains(Set.of("first", "second"));
    }
}
