package backend.academy.scrapper.client.puppettheatre;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class PuppetTheatreBackoffTest {
    private final MutableClock clock = new MutableClock(Instant.parse("2026-09-14T10:00:00Z"));
    private final PuppetTheatreBackoff backoff = new PuppetTheatreBackoff(clock);

    @Test
    void shouldRetryFirstBlockingFailureOnTheRegularSchedulerInterval() {
        Duration delay = backoff.recordBlockingFailure(Duration.ZERO);

        assertThat(delay).isZero();
        assertThat(backoff.requestAllowed()).isTrue();
    }

    @Test
    void shouldEscalateRepeatedBlockingFailuresAndCapAtOneHour() {
        assertThat(backoff.recordBlockingFailure(Duration.ZERO)).isZero();
        assertDelay(Duration.ofMinutes(2));
        assertDelay(Duration.ofMinutes(5));
        assertDelay(Duration.ofMinutes(15));
        assertDelay(Duration.ofMinutes(30));
        assertDelay(Duration.ofHours(1));
        assertDelay(Duration.ofHours(1));
    }

    @Test
    void shouldHonorLongerRetryAfterOnFirstFailure() {
        Duration delay = backoff.recordBlockingFailure(Duration.ofMinutes(8));

        assertThat(delay).isEqualTo(Duration.ofMinutes(8));
        assertThat(backoff.requestAllowed()).isFalse();
        clock.advance(Duration.ofMinutes(8));
        assertThat(backoff.requestAllowed()).isTrue();
    }

    @Test
    void shouldResetEscalationOnlyAfterThreeStableSuccesses() {
        backoff.recordBlockingFailure(Duration.ZERO);
        backoff.recordBlockingFailure(Duration.ZERO);
        clock.advance(Duration.ofMinutes(2));

        backoff.recordSuccess();
        backoff.recordSuccess();
        assertThat(backoff.recordBlockingFailure(Duration.ZERO)).isEqualTo(Duration.ofMinutes(5));
        clock.advance(Duration.ofMinutes(5));
        backoff.recordSuccess();
        backoff.recordSuccess();
        backoff.recordSuccess();

        assertThat(backoff.recordBlockingFailure(Duration.ZERO)).isZero();
    }

    private void assertDelay(Duration expected) {
        Duration actual = backoff.recordBlockingFailure(Duration.ZERO);
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
