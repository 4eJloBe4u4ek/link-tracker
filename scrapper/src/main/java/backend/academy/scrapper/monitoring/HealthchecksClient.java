package backend.academy.scrapper.monitoring;

import backend.academy.scrapper.config.MonitoringProperties;
import backend.academy.shared.monitoring.HealthchecksHeartbeat;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class HealthchecksClient {
    private static final String SCRAPPER_MONITOR_NAME = "scrapper";
    private static final String TICKETPRO_MONITOR_NAME = "ticketpro";
    private static final String PUPPET_THEATRE_MONITOR_NAME = "puppet-theatre";

    private final HealthchecksHeartbeat scrapperHeartbeat;
    private final HealthchecksHeartbeat ticketproHeartbeat;
    private final HealthchecksHeartbeat puppetTheatreHeartbeat;

    @Autowired
    public HealthchecksClient(
            MonitoringProperties properties, WebClient.Builder webClientBuilder, MeterRegistry meterRegistry) {
        this(properties, webClientBuilder, meterRegistry, Clock.systemUTC());
    }

    HealthchecksClient(
            MonitoringProperties properties,
            WebClient.Builder webClientBuilder,
            MeterRegistry meterRegistry,
            Clock clock) {
        String scrapperUrl = properties.enabled() ? properties.scrapperPingUrl() : "";
        String ticketproUrl = properties.enabled() ? properties.ticketproPingUrl() : "";
        String puppetTheatreUrl = properties.enabled() ? properties.puppetTheatrePingUrl() : "";
        this.scrapperHeartbeat =
                new HealthchecksHeartbeat(SCRAPPER_MONITOR_NAME, scrapperUrl, webClientBuilder, meterRegistry, clock);
        this.ticketproHeartbeat =
                new HealthchecksHeartbeat(TICKETPRO_MONITOR_NAME, ticketproUrl, webClientBuilder, meterRegistry, clock);
        this.puppetTheatreHeartbeat = new HealthchecksHeartbeat(
                PUPPET_THEATRE_MONITOR_NAME, puppetTheatreUrl, webClientBuilder, meterRegistry, clock);
    }

    public void pingScrapper() {
        scrapperHeartbeat.ping();
    }

    public void pingTicketpro() {
        ticketproHeartbeat.ping();
    }

    public void pingPuppetTheatre() {
        puppetTheatreHeartbeat.ping();
    }
}
