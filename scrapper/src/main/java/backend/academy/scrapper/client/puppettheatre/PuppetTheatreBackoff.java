package backend.academy.scrapper.client.puppettheatre;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;

public final class PuppetTheatreBackoff {
    private static final double MAXIMUM_JITTER = 0.1;

    private final Clock clock;
    private final DoubleSupplier jitterSupplier;
    private Instant blockedUntil;
    private BackoffPolicy activePolicy;
    private int consecutiveFailures;

    public PuppetTheatreBackoff(Clock clock) {
        this(clock, () -> ThreadLocalRandom.current().nextDouble(-MAXIMUM_JITTER, MAXIMUM_JITTER));
    }

    PuppetTheatreBackoff(Clock clock, DoubleSupplier jitterSupplier) {
        this.clock = clock;
        this.jitterSupplier = jitterSupplier;
    }

    public synchronized boolean requestAllowed() {
        return blockedUntil == null || !clock.instant().isBefore(blockedUntil);
    }

    public synchronized Duration recordFailure(PuppetTheatreCheckResult result, Duration retryAfter) {
        BackoffPolicy policy = BackoffPolicy.forResult(result);
        if (policy != activePolicy) {
            activePolicy = policy;
            consecutiveFailures = 0;
        }
        consecutiveFailures++;

        Duration adaptive = applyJitter(policy.delayFor(consecutiveFailures));
        Duration requested = retryAfter == null || retryAfter.isNegative() ? Duration.ZERO : retryAfter;
        Duration delay = adaptive.compareTo(requested) >= 0 ? adaptive : requested;
        blockedUntil = delay.isZero() ? null : clock.instant().plus(delay);
        return delay;
    }

    public synchronized void recordSuccess() {
        blockedUntil = null;
        activePolicy = null;
        consecutiveFailures = 0;
    }

    private Duration applyJitter(Duration delay) {
        double suppliedJitter = jitterSupplier.getAsDouble();
        double boundedJitter =
                Double.isFinite(suppliedJitter) ? Math.clamp(suppliedJitter, -MAXIMUM_JITTER, MAXIMUM_JITTER) : 0;
        return Duration.ofMillis(Math.max(0, Math.round(delay.toMillis() * (1 + boundedJitter))));
    }

    private enum BackoffPolicy {
        TRANSIENT(Duration.ofMinutes(1), Duration.ofMinutes(2), Duration.ofMinutes(5)),
        FORBIDDEN(Duration.ofMinutes(2), Duration.ofMinutes(5), Duration.ofMinutes(10)),
        RATE_LIMIT(Duration.ofMinutes(5), Duration.ofMinutes(15), Duration.ofMinutes(30));

        private final Duration firstDelay;
        private final Duration secondDelay;
        private final Duration maximumDelay;

        BackoffPolicy(Duration firstDelay, Duration secondDelay, Duration maximumDelay) {
            this.firstDelay = firstDelay;
            this.secondDelay = secondDelay;
            this.maximumDelay = maximumDelay;
        }

        private static BackoffPolicy forResult(PuppetTheatreCheckResult result) {
            return switch (result) {
                case HTTP_ERROR, NETWORK_ERROR, TIMEOUT -> TRANSIENT;
                case ANTIBOT, HTTP_403 -> FORBIDDEN;
                case HTTP_429 -> RATE_LIMIT;
                default -> throw new IllegalArgumentException("No backoff policy for " + result);
            };
        }

        private Duration delayFor(int failureNumber) {
            return switch (failureNumber) {
                case 1 -> firstDelay;
                case 2 -> secondDelay;
                default -> maximumDelay;
            };
        }
    }
}
