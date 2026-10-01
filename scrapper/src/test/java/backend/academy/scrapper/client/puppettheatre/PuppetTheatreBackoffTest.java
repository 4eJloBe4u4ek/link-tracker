package backend.academy.scrapper.client.puppettheatre;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class PuppetTheatreBackoffTest {
    private final MutableClock clock = new MutableClock(Instant.parse("2026-09-14T10:00:00Z"));
    private final PuppetTheatreBackoff backoff = new PuppetTheatreBackoff(clock, () -> 0);

    @Test
    void shouldBackOffTransientFailuresAtOneTwoAndFiveMinutes() {
        assertDelay(PuppetTheatreCheckResult.HTTP_ERROR, Duration.ofMinutes(1));
        assertDelay(PuppetTheatreCheckResult.TIMEOUT, Duration.ofMinutes(2));
        assertDelay(PuppetTheatreCheckResult.NETWORK_ERROR, Duration.ofMinutes(5));
        assertDelay(PuppetTheatreCheckResult.HTTP_ERROR, Duration.ofMinutes(5));
    }

    @Test
    void shouldBackOffForbiddenResponsesAtTwoFiveAndTenMinutes() {
        assertDelay(PuppetTheatreCheckResult.HTTP_403, Duration.ofMinutes(2));
        assertDelay(PuppetTheatreCheckResult.ANTIBOT, Duration.ofMinutes(5));
        assertDelay(PuppetTheatreCheckResult.HTTP_403, Duration.ofMinutes(10));
        assertDelay(PuppetTheatreCheckResult.HTTP_403, Duration.ofMinutes(10));
    }

    @Test
    void shouldBackOffRateLimitsAtFiveFifteenAndThirtyMinutes() {
        assertDelay(PuppetTheatreCheckResult.HTTP_429, Duration.ofMinutes(5));
        assertDelay(PuppetTheatreCheckResult.HTTP_429, Duration.ofMinutes(15));
        assertDelay(PuppetTheatreCheckResult.HTTP_429, Duration.ofMinutes(30));
        assertDelay(PuppetTheatreCheckResult.HTTP_429, Duration.ofMinutes(30));
    }

    @Test
    void shouldHonorRetryAfterWhenItIsLongerThanTheAdaptiveDelay() {
        Duration delay = backoff.recordFailure(PuppetTheatreCheckResult.HTTP_429, Duration.ofHours(2));

        assertThat(delay).isEqualTo(Duration.ofHours(2));
        assertThat(backoff.requestAllowed()).isFalse();
        clock.advance(Duration.ofHours(2));
        assertThat(backoff.requestAllowed()).isTrue();
    }

    @Test
    void shouldRestartEscalationWhenFailurePolicyChanges() {
        assertDelay(PuppetTheatreCheckResult.HTTP_ERROR, Duration.ofMinutes(1));
        assertDelay(PuppetTheatreCheckResult.HTTP_ERROR, Duration.ofMinutes(2));
        assertDelay(PuppetTheatreCheckResult.HTTP_403, Duration.ofMinutes(2));
        assertDelay(PuppetTheatreCheckResult.HTTP_ERROR, Duration.ofMinutes(1));
    }

    @Test
    void shouldResetEscalationAfterOneSuccessfulCheck() {
        assertDelay(PuppetTheatreCheckResult.HTTP_403, Duration.ofMinutes(2));
        assertDelay(PuppetTheatreCheckResult.HTTP_403, Duration.ofMinutes(5));

        backoff.recordSuccess();

        assertDelay(PuppetTheatreCheckResult.HTTP_403, Duration.ofMinutes(2));
    }

    @Test
    void shouldApplyBoundedJitterWithoutShorteningRetryAfter() {
        PuppetTheatreBackoff positiveJitter = new PuppetTheatreBackoff(clock, () -> 0.5);
        PuppetTheatreBackoff negativeJitter = new PuppetTheatreBackoff(clock, () -> -0.5);

        assertThat(positiveJitter.recordFailure(PuppetTheatreCheckResult.HTTP_ERROR, Duration.ZERO))
                .isEqualTo(Duration.ofSeconds(66));
        assertThat(negativeJitter.recordFailure(PuppetTheatreCheckResult.HTTP_ERROR, Duration.ofMinutes(1)))
                .isEqualTo(Duration.ofMinutes(1));
    }

    private void assertDelay(PuppetTheatreCheckResult result, Duration expected) {
        Duration actual = backoff.recordFailure(result, Duration.ZERO);
        assertThat(actual).isEqualTo(expected);
        assertThat(backoff.requestAllowed()).isFalse();
        clock.advance(expected);
        assertThat(backoff.requestAllowed()).isTrue();
    }

    private static final class MutableClock extends Clock {
        private Instant current;

        private MutableClock(Instant current) {
            this.current = current;
        }

        private void advance(Duration duration) {
            current = current.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current;
        }
    }
}
