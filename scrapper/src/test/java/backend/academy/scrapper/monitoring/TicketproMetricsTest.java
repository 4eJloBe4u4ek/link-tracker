package backend.academy.scrapper.monitoring;

import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.scrapper.client.ticketpro.TicketproCheckResult;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TicketproMetricsTest {
    private static final Instant CHECK_TIME = Instant.parse("2026-08-10T12:00:00Z");

    private SimpleMeterRegistry meterRegistry;
    private TicketproMetrics metrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metrics = new TicketproMetrics(meterRegistry, Clock.fixed(CHECK_TIME, ZoneOffset.UTC));
    }

    @Test
    void shouldRecordFailuresByCategoryAndTrackConsecutiveFailures() {
        // Arrange
        TicketproCheckResult result = TicketproCheckResult.ANTIBOT;

        // Act
        metrics.recordFailure(result);
        metrics.recordFailure(result);

        // Assert
        assertThat(meterRegistry.counter("ticketpro_check_total", "result", "antibot").count())
                .isEqualTo(2);
        assertThat(meterRegistry.get("ticketpro_consecutive_failures").gauge().value())
                .isEqualTo(2);
    }

    @Test
    void shouldResetFailuresAndUpdateSuccessState() {
        // Arrange
        metrics.recordFailure(TicketproCheckResult.TIMEOUT);

        // Act
        metrics.recordSuccess(3);

        // Assert
        assertThat(meterRegistry.counter("ticketpro_check_total", "result", "success").count())
                .isEqualTo(1);
        assertThat(meterRegistry.get("ticketpro_consecutive_failures").gauge().value())
                .isZero();
        assertThat(meterRegistry.get("ticketpro_last_success_timestamp_seconds").gauge().value())
                .isEqualTo((double) CHECK_TIME.getEpochSecond());
        assertThat(meterRegistry.get("ticketpro_available_sessions").gauge().value())
                .isEqualTo(3);
    }
}
