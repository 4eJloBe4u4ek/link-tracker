package backend.academy.scrapper.config.metrics;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import backend.academy.scrapper.repository.LinkOperationRepository;
import backend.academy.shared.dto.LinkType;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

class LinkMetricsTest {
    SimpleMeterRegistry registry;
    LinkOperationRepository linkOperationRepository = mock(LinkOperationRepository.class);
    LinkMetrics metricsConfig = new LinkMetrics();

    @SneakyThrows
    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        Metrics.globalRegistry.add(registry);
        when(linkOperationRepository.countByType(LinkType.GITHUB)).thenReturn(123L);
        when(linkOperationRepository.countByType(LinkType.STACKOVERFLOW)).thenReturn(321L);

        ApplicationRunner runner = metricsConfig.registerMetrics(registry, linkOperationRepository);
        runner.run(mock(ApplicationArguments.class));
    }

    @AfterEach
    void tearDown() {
        registry.clear();
        Metrics.globalRegistry.clear();
    }

    @Test
    void shouldRegisterGaugesCorrectly() {
        Gauge githubGauge =
                registry.find("custom_active_links").tag("type", "github").gauge();
        Gauge stackoverflowGauge = registry.find("custom_active_links")
                .tag("type", "stackoverflow")
                .gauge();

        Assertions.assertNotNull(githubGauge);
        Assertions.assertNotNull(stackoverflowGauge);

        Assertions.assertEquals(123L, githubGauge.value());
        Assertions.assertEquals(321L, stackoverflowGauge.value());
    }
}
