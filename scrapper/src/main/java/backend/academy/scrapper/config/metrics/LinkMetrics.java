package backend.academy.scrapper.config.metrics;

import backend.academy.scrapper.repository.LinkOperationRepository;
import backend.academy.shared.dto.LinkType;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LinkMetrics {
    private static final String ACTIVE_LINKS_GAUGE_NAME = "custom_active_links";
    private static final String ACTIVE_LINKS_GAUGE_TAG_TYPE = "type";

    @Bean
    public ApplicationRunner registerMetrics(
            MeterRegistry meterRegistry, LinkOperationRepository linkOperationRepository) {
        return args -> {
            Gauge.builder(ACTIVE_LINKS_GAUGE_NAME, linkOperationRepository, l -> l.countByType(LinkType.GITHUB))
                    .tag(ACTIVE_LINKS_GAUGE_TAG_TYPE, LinkType.GITHUB.name().toLowerCase())
                    .register(meterRegistry);

            Gauge.builder(ACTIVE_LINKS_GAUGE_NAME, linkOperationRepository, l -> l.countByType(LinkType.STACKOVERFLOW))
                    .tag(
                            ACTIVE_LINKS_GAUGE_TAG_TYPE,
                            LinkType.STACKOVERFLOW.name().toLowerCase())
                    .register(meterRegistry);
        };
    }
}
