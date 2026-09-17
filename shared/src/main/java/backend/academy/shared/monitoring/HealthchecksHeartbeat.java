package backend.academy.shared.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
public final class HealthchecksHeartbeat {
    private static final Duration MINIMUM_PING_INTERVAL = Duration.ofMinutes(1);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(3);
    private static final String HEARTBEAT_METRIC_NAME = "monitoring_heartbeat_total";

    private final String name;
    private final String pingUrl;
    private final WebClient webClient;
    private final MeterRegistry meterRegistry;
    private final Clock clock;
    private final AtomicReference<Instant> lastAttempt = new AtomicReference<>();

    public HealthchecksHeartbeat(
            String name, String pingUrl, WebClient.Builder webClientBuilder, MeterRegistry meterRegistry, Clock clock) {
        this.name = name;
        this.pingUrl = pingUrl;
        this.webClient = webClientBuilder
                .clone()
                .observationRegistry(ObservationRegistry.NOOP)
                .build();
        this.meterRegistry = meterRegistry;
        this.clock = clock;
    }

    public boolean isEnabled() {
        return pingUrl != null && !pingUrl.isBlank();
    }

    public void ping() {
        if (!isEnabled() || !tryAcquire()) {
            return;
        }

        try {
            webClient
                    .get()
                    .uri(pingUrl)
                    .exchangeToMono(response -> response.statusCode().is2xxSuccessful()
                            ? response.releaseBody()
                            : response.releaseBody().then(Mono.error(new HeartbeatDeliveryException())))
                    .timeout(REQUEST_TIMEOUT)
                    .doOnSuccess(ignored -> recordResult("success"))
                    .subscribe(ignored -> {}, this::recordFailure);
        } catch (RuntimeException exception) {
            recordFailure(exception);
        }
    }

    private boolean tryAcquire() {
        Instant now = clock.instant();
        while (true) {
            Instant previous = lastAttempt.get();
            if (previous != null && now.isBefore(previous.plus(MINIMUM_PING_INTERVAL))) {
                return false;
            }
            if (lastAttempt.compareAndSet(previous, now)) {
                return true;
            }
        }
    }

    private void recordFailure(Throwable error) {
        recordResult("failure");
        log.warn("Failed to deliver {} heartbeat: {}", name, error.getClass().getSimpleName());
    }

    private void recordResult(String result) {
        meterRegistry
                .counter(HEARTBEAT_METRIC_NAME, "monitor", name, "result", result)
                .increment();
    }

    private static final class HeartbeatDeliveryException extends RuntimeException {}
}
