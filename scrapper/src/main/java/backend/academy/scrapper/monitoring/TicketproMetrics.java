package backend.academy.scrapper.monitoring;

import backend.academy.scrapper.client.puppettheatre.TicketproCheckResult;
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
public class TicketproMetrics {
    private static final String CHECK_COUNTER_NAME = "ticketpro_check_total";
    private static final String RESULT_TAG_NAME = "result";

    private final MeterRegistry meterRegistry;
    private final Clock clock;
    private final AtomicInteger consecutiveFailures = new AtomicInteger();
    private final AtomicLong lastSuccessTimestampSeconds = new AtomicLong();
    private final AtomicInteger availableSessions = new AtomicInteger();

    @Autowired
    public TicketproMetrics(MeterRegistry meterRegistry) {
        this(meterRegistry, Clock.systemUTC());
    }

    TicketproMetrics(MeterRegistry meterRegistry, Clock clock) {
        this.meterRegistry = meterRegistry;
        this.clock = clock;
        Gauge.builder("ticketpro_consecutive_failures", consecutiveFailures, AtomicInteger::get)
                .register(meterRegistry);
        Gauge.builder("ticketpro_last_success_timestamp_seconds", lastSuccessTimestampSeconds, AtomicLong::get)
                .register(meterRegistry);
        Gauge.builder("ticketpro_available_sessions", availableSessions, AtomicInteger::get)
                .register(meterRegistry);
    }

    public void recordSuccess(int sessionCount) {
        meterRegistry
                .counter(CHECK_COUNTER_NAME, RESULT_TAG_NAME, TicketproCheckResult.SUCCESS.metricValue())
                .increment();
        int previousFailures = consecutiveFailures.getAndSet(0);
        lastSuccessTimestampSeconds.set(clock.instant().getEpochSecond());
        availableSessions.set(sessionCount);
        if (previousFailures > 0) {
            log.info("Ticketpro checks recovered after {} consecutive failures", previousFailures);
        }
    }

    public void recordFailure(TicketproCheckResult result) {
        meterRegistry
                .counter(CHECK_COUNTER_NAME, RESULT_TAG_NAME, result.metricValue())
                .increment();
        int previousFailures = consecutiveFailures.getAndIncrement();
        if (previousFailures == 0) {
            log.warn("Ticketpro checks started failing: {}", result.metricValue());
        }
    }
}
