package backend.academy.bot.monitoring;

import backend.academy.bot.config.MonitoringProperties;
import backend.academy.shared.monitoring.HealthchecksHeartbeat;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class HealthchecksClient {
    private static final String MONITOR_NAME = "bot";

    private final HealthchecksHeartbeat heartbeat;

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
        this.heartbeat = new HealthchecksHeartbeat(
                MONITOR_NAME, properties.botPingUrl(), webClientBuilder, meterRegistry, clock);
    }

    public void pingBot() {
        heartbeat.ping();
    }

    public boolean isEnabled() {
        return heartbeat.isEnabled();
    }
}
