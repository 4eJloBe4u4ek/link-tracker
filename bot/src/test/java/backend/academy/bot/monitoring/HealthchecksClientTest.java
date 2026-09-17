package backend.academy.bot.monitoring;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import backend.academy.bot.config.MonitoringProperties;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.reactive.function.client.WebClient;

class HealthchecksClientTest {
    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @Test
    void shouldUseConfiguredBotHeartbeat() {
        // Arrange
        wireMock.stubFor(get("/bot").willReturn(aResponse().withStatus(200)));
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        HealthchecksClient client = new HealthchecksClient(
                new MonitoringProperties(true, wireMock.url("/bot")),
                WebClient.builder(),
                meterRegistry,
                Clock.systemUTC());

        // Act
        client.pingBot();

        // Assert
        assertThat(client.isEnabled()).isTrue();
        await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
            wireMock.verify(1, getRequestedFor(urlEqualTo("/bot")));
            assertThat(meterRegistry
                            .counter("monitoring_heartbeat_total", "monitor", "bot", "result", "success")
                            .count())
                    .isEqualTo(1);
        });
    }

    @Test
    void shouldDisableBotHeartbeatEvenWhenUrlIsPresent() {
        HealthchecksClient client = new HealthchecksClient(
                new MonitoringProperties(false, wireMock.url("/bot")),
                WebClient.builder(),
                new SimpleMeterRegistry(),
                Clock.systemUTC());

        client.pingBot();

        assertThat(client.isEnabled()).isFalse();
        wireMock.verify(0, getRequestedFor(urlEqualTo("/bot")));
    }
}
