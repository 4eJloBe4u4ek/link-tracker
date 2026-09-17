package backend.academy.scrapper.monitoring;

import backend.academy.scrapper.client.puppettheatre.PuppetTheatreCheckResult;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PuppetTheatreMetrics {
    public static final String SOURCE_TYPE = "puppet_theatre";

    private static final String CHECK_COUNTER_NAME = "puppet_theatre_check_total";

    private final MeterRegistry meterRegistry;
    private final Clock clock;
    private final AtomicInteger consecutiveFailures = new AtomicInteger();
    private final AtomicLong lastSuccessTimestampSeconds = new AtomicLong();
    private final AtomicInteger availableSessions = new AtomicInteger();
    private final AtomicInteger availableMonths = new AtomicInteger();

    @Autowired
    public PuppetTheatreMetrics(MeterRegistry meterRegistry) {
        this(meterRegistry, Clock.systemUTC());
    }

    PuppetTheatreMetrics(MeterRegistry meterRegistry, Clock clock) {
        this.meterRegistry = meterRegistry;
        this.clock = clock;
        Gauge.builder("puppet_theatre_consecutive_failures", consecutiveFailures, AtomicInteger::get)
                .register(meterRegistry);
        Gauge.builder("puppet_theatre_last_success_timestamp_seconds", lastSuccessTimestampSeconds, AtomicLong::get)
                .register(meterRegistry);
        Gauge.builder("puppet_theatre_available_sessions", availableSessions, AtomicInteger::get)
                .register(meterRegistry);
        Gauge.builder("puppet_theatre_available_months", availableMonths, AtomicInteger::get)
                .register(meterRegistry);
    }

    public void recordSuccess(int sessionCount, int monthCount) {
        meterRegistry
                .counter(CHECK_COUNTER_NAME, "result", PuppetTheatreCheckResult.SUCCESS.metricValue())
                .increment();
        int previousFailures = consecutiveFailures.getAndSet(0);
        lastSuccessTimestampSeconds.set(clock.instant().getEpochSecond());
        availableSessions.set(sessionCount);
        availableMonths.set(monthCount);
        if (previousFailures > 0) {
            log.info("Puppet theatre checks recovered after {} consecutive failures", previousFailures);
        }
    }

    public void recordFailure(PuppetTheatreCheckResult result) {
        meterRegistry
                .counter(CHECK_COUNTER_NAME, "result", result.metricValue())
                .increment();
        int previousFailures = consecutiveFailures.getAndIncrement();
        if (previousFailures == 0) {
            log.warn("Puppet theatre checks started failing: {}", result.metricValue());
        }
    }
}
