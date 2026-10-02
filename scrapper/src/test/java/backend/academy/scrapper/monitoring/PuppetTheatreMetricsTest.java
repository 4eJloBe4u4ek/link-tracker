package backend.academy.scrapper.monitoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import backend.academy.scrapper.client.puppettheatre.PuppetTheatreBackoff.BackoffState;
import backend.academy.scrapper.client.puppettheatre.PuppetTheatreCheckResult;
import backend.academy.scrapper.client.puppettheatre.PuppetTheatreClient;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class PuppetTheatreMetricsTest {
    private static final Instant NOW = Instant.parse("2026-10-02T08:35:29Z");

    @Test
    void shouldExposeActiveBackoffUntilAndReason() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        PuppetTheatreClient client = mock(PuppetTheatreClient.class);
        AtomicReference<BackoffState> state =
                new AtomicReference<>(new BackoffState(NOW.plusSeconds(600), PuppetTheatreCheckResult.HTTP_ERROR));
        when(client.backoffState()).thenAnswer(ignored -> state.get());
        new PuppetTheatreMetrics(registry, client, Clock.fixed(NOW, ZoneOffset.UTC));

        assertThat(registry.get("puppet_theatre_backoff_until_timestamp_seconds")
                        .gauge()
                        .value())
                .isEqualTo((double) NOW.plusSeconds(600).getEpochSecond());
        assertThat(registry.get("puppet_theatre_backoff_reason")
                        .tag("reason", "http_error")
                        .gauge()
                        .value())
                .isEqualTo(1);
        assertThat(registry.get("puppet_theatre_backoff_reason")
                        .tag("reason", "timeout")
                        .gauge()
                        .value())
                .isZero();

        state.set(new BackoffState(NOW, PuppetTheatreCheckResult.HTTP_ERROR));

        assertThat(registry.get("puppet_theatre_backoff_until_timestamp_seconds")
                        .gauge()
                        .value())
                .isZero();
        assertThat(registry.get("puppet_theatre_backoff_reason")
                        .tag("reason", "http_error")
                        .gauge()
                        .value())
                .isZero();
    }
}
