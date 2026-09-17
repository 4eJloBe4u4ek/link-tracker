package backend.academy.scrapper.client.puppettheatre;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

public final class PuppetTheatreBackoff {
    private static final Duration MAXIMUM_DELAY = Duration.ofHours(1);
    private static final Duration[] ADAPTIVE_DELAYS = {
        Duration.ZERO,
        Duration.ofMinutes(2),
        Duration.ofMinutes(5),
        Duration.ofMinutes(15),
        Duration.ofMinutes(30),
        MAXIMUM_DELAY
    };
    private static final int STABLE_SUCCESSES_TO_RESET = 3;

    private final Clock clock;
    private Instant blockedUntil;
    private int blockingFailures;
    private int stableSuccesses;

    public PuppetTheatreBackoff(Clock clock) {
        this.clock = clock;
    }

    public synchronized boolean requestAllowed() {
        return blockedUntil == null || !clock.instant().isBefore(blockedUntil);
    }

    public synchronized Duration recordBlockingFailure(Duration retryAfter) {
        stableSuccesses = 0;
        blockingFailures++;
        Duration adaptive = ADAPTIVE_DELAYS[Math.min(blockingFailures - 1, ADAPTIVE_DELAYS.length - 1)];
        Duration requested = retryAfter == null || retryAfter.isNegative() ? Duration.ZERO : retryAfter;
        Duration delay = adaptive.compareTo(requested) >= 0 ? adaptive : requested;
        if (delay.compareTo(MAXIMUM_DELAY) > 0) {
            delay = MAXIMUM_DELAY;
        }
        blockedUntil = delay.isZero() ? null : clock.instant().plus(delay);
        return delay;
    }

    public synchronized void recordSuccess() {
        blockedUntil = null;
        if (blockingFailures == 0) {
            return;
        }
        stableSuccesses++;
        if (stableSuccesses >= STABLE_SUCCESSES_TO_RESET) {
            stableSuccesses = 0;
            blockingFailures = 0;
        }
    }
}
