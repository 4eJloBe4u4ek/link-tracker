package backend.academy.scrapper.scheduler.handler;

import static backend.academy.scrapper.TestData.GITHUB_TRACKED_LINK;
import static backend.academy.scrapper.TestData.STACKOVERFLOW_TRACKED_LINK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import backend.academy.scrapper.client.github.GithubClient;
import backend.academy.scrapper.client.stackoverflow.StackOverflowClient;
import backend.academy.scrapper.scheduler.service.NotificationService;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class ScrapeTimeMetricsTest {
    private SimpleMeterRegistry registry;
    private GithubUpdateHandler githubUpdateHandler;
    private StackOverflowUpdateHandler stackOverflowUpdateHandler;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        Metrics.globalRegistry.add(registry);

        NotificationService notificationService = mock(NotificationService.class);

        GithubClient githubClient = mock(GithubClient.class);
        when(githubClient.getRepositoryUpdates(anyString(), anyString(), any())).thenReturn(Mono.empty());
        githubUpdateHandler = new GithubUpdateHandler(githubClient, notificationService, registry);

        StackOverflowClient stackOverflowClient = mock(StackOverflowClient.class);
        when(stackOverflowClient.getStackoverflowUpdates(anyLong(), any())).thenReturn(Mono.empty());
        stackOverflowUpdateHandler = new StackOverflowUpdateHandler(notificationService, stackOverflowClient, registry);
    }

    @AfterEach
    void tearDown() {
        registry.clear();
        Metrics.globalRegistry.clear();
    }

    @Test
    void shouldRegisterGithubCustomScrapeTimeMetricsCorrectly() {
        // Act
        CompletableFuture<Void> future = githubUpdateHandler.handle(GITHUB_TRACKED_LINK);
        future.join();

        // Assert
        Timer timer = registry.find("custom_scrape_time").tag("type", "github").timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
    }

    @Test
    void shouldRegisterStackoverflowCustomScrapeTimeMetricsCorrectly() {
        // Act
        CompletableFuture<Void> future = stackOverflowUpdateHandler.handle(STACKOVERFLOW_TRACKED_LINK);
        future.join();

        // Assert
        Timer timer =
                registry.find("custom_scrape_time").tag("type", "stackoverflow").timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
    }
}
