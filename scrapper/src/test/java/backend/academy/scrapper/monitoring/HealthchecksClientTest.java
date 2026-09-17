package backend.academy.scrapper.monitoring;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import backend.academy.scrapper.config.MonitoringProperties;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.reactive.function.client.WebClient;

class HealthchecksClientTest {
    private static final Instant NOW = Instant.parse("2026-08-10T15:00:00Z");

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @Test
    void shouldUseIndependentConfiguredHeartbeats() {
        // Arrange
        wireMock.stubFor(get("/scrapper").willReturn(aResponse().withStatus(200)));
        wireMock.stubFor(get("/ticketpro").willReturn(aResponse().withStatus(200)));
        wireMock.stubFor(get("/puppet").willReturn(aResponse().withStatus(200)));
        Clock clock = mock(Clock.class);
        when(clock.instant()).thenReturn(NOW, NOW, NOW.plusSeconds(30), NOW.plusSeconds(30));
        HealthchecksClient client = new HealthchecksClient(
                new MonitoringProperties(
                        true, wireMock.url("/scrapper"), wireMock.url("/ticketpro"), wireMock.url("/puppet")),
                WebClient.builder(),
                new SimpleMeterRegistry(),
                clock);

        // Act
        client.pingScrapper();
        client.pingTicketpro();
        client.pingPuppetTheatre();
        client.pingScrapper();
        client.pingTicketpro();

        // Assert
        await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
            wireMock.verify(1, getRequestedFor(urlEqualTo("/scrapper")));
            wireMock.verify(1, getRequestedFor(urlEqualTo("/ticketpro")));
            wireMock.verify(1, getRequestedFor(urlEqualTo("/puppet")));
        });
    }

    @Test
    void shouldDisableAllScrapperHeartbeatsEvenWhenUrlsArePresent() {
        HealthchecksClient client = new HealthchecksClient(
                new MonitoringProperties(
                        false, wireMock.url("/scrapper"), wireMock.url("/ticketpro"), wireMock.url("/puppet")),
                WebClient.builder(),
                new SimpleMeterRegistry(),
                Clock.systemUTC());

        client.pingScrapper();
        client.pingTicketpro();
        client.pingPuppetTheatre();

        wireMock.verify(0, getRequestedFor(com.github.tomakehurst.wiremock.client.WireMock.anyUrl()));
    }
}
