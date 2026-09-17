package backend.academy.shared.monitoring;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.reactive.function.client.WebClient;

class HealthchecksHeartbeatTest {
    private static final Instant NOW = Instant.parse("2026-08-10T15:00:00Z");

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @Test
    void shouldSendAtMostOneHeartbeatPerMinute() {
        // Arrange
        wireMock.stubFor(get("/heartbeat").willReturn(aResponse().withStatus(200)));
        Clock clock = mock(Clock.class);
        when(clock.instant()).thenReturn(NOW, NOW.plusSeconds(30), NOW.plusSeconds(60));
        HealthchecksHeartbeat heartbeat = new HealthchecksHeartbeat(
                "service", wireMock.url("/heartbeat"), WebClient.builder(), new SimpleMeterRegistry(), clock);

        // Act
        heartbeat.ping();
        heartbeat.ping();
        heartbeat.ping();

        // Assert
        await().atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> wireMock.verify(2, getRequestedFor(urlEqualTo("/heartbeat"))));
    }

    @Test
    void shouldBeDisabledAndSendNothingWhenUrlIsBlank() {
        // Arrange
        HealthchecksHeartbeat heartbeat = new HealthchecksHeartbeat(
                "service", "", WebClient.builder(), new SimpleMeterRegistry(), Clock.systemUTC());

        // Act
        heartbeat.ping();

        // Assert
        assertThat(heartbeat.isEnabled()).isFalse();
        wireMock.verify(0, getRequestedFor(urlEqualTo("/heartbeat")));
    }

    @Test
    void shouldContainDeliveryFailureAndRecordMetric() {
        // Arrange
        wireMock.stubFor(get("/failure").willReturn(aResponse().withStatus(500)));
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        HealthchecksHeartbeat heartbeat = new HealthchecksHeartbeat(
                "service", wireMock.url("/failure"), WebClient.builder(), registry, Clock.systemUTC());

        // Act & Assert
        assertThatCode(heartbeat::ping).doesNotThrowAnyException();
        await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> assertThat(
                        registry.counter("monitoring_heartbeat_total", "monitor", "service", "result", "failure")
                                .count())
                .isEqualTo(1));
    }

    @Test
    void shouldContainInvalidUrl() {
        // Arrange
        HealthchecksHeartbeat heartbeat = new HealthchecksHeartbeat(
                "service", "http://[invalid", WebClient.builder(), new SimpleMeterRegistry(), Clock.systemUTC());

        // Act & Assert
        assertThatCode(heartbeat::ping).doesNotThrowAnyException();
    }

    @Test
    void shouldNotExposePingUrlThroughHttpObservations() {
        // Arrange
        wireMock.stubFor(get("/secret-ping").willReturn(aResponse().withStatus(200)));
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        ObservationRegistry observationRegistry = ObservationRegistry.create();
        observationRegistry.observationConfig().observationHandler(new DefaultMeterObservationHandler(meterRegistry));
        HealthchecksHeartbeat heartbeat = new HealthchecksHeartbeat(
                "service",
                wireMock.url("/secret-ping"),
                WebClient.builder().observationRegistry(observationRegistry),
                meterRegistry,
                Clock.systemUTC());

        // Act
        heartbeat.ping();

        // Assert
        await().atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> wireMock.verify(1, getRequestedFor(urlEqualTo("/secret-ping"))));
        assertThat(meterRegistry.find("http.client.requests").meters()).isEmpty();
    }
}
