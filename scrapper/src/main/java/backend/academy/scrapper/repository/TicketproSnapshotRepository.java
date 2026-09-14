package backend.academy.scrapper.repository;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TicketproSnapshotRepository {
    private static final String KEY_PREFIX = "ticketpro:";
    private static final String SNAPSHOT_SUFFIX = ":snapshot";
    private static final String EVENT_SEPARATOR = "\n";
    private static final Duration SNAPSHOT_TTL = Duration.ofDays(90);

    private final StringRedisTemplate redisTemplate;

    public Optional<Set<String>> getAvailableEventKeys(Long linkId) {
        String value = redisTemplate.opsForValue().get(snapshotKey(linkId));
        if (value == null) {
            return Optional.empty();
        }
        if (value.isEmpty()) {
            return Optional.of(Set.of());
        }
        return Optional.of(Arrays.stream(value.split(EVENT_SEPARATOR, -1)).collect(Collectors.toUnmodifiableSet()));
    }

    public void replaceAvailableEventKeys(Long linkId, Set<String> eventKeys) {
        String value = String.join(EVENT_SEPARATOR, eventKeys);
        redisTemplate.opsForValue().set(snapshotKey(linkId), value, SNAPSHOT_TTL);
    }

    private String snapshotKey(Long linkId) {
        return KEY_PREFIX + linkId + SNAPSHOT_SUFFIX;
    }
}
