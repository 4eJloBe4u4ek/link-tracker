package backend.academy.scrapper.scheduler;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import backend.academy.scrapper.monitoring.HealthchecksClient;
import backend.academy.scrapper.scheduler.service.LinkUpdateService;
import backend.academy.scrapper.scheduler.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.dao.DataAccessResourceFailureException;

class UpdateSchedulerTest {
    @Test
    void shouldPingScrapperAfterSuccessfulUpdateCycle() {
        // Arrange
        LinkUpdateService linkUpdateService = mock(LinkUpdateService.class);
        NotificationService notificationService = mock(NotificationService.class);
        HealthchecksClient healthchecksClient = mock(HealthchecksClient.class);
        UpdateScheduler scheduler = new UpdateScheduler(linkUpdateService, notificationService, healthchecksClient);

        // Act
        scheduler.checkUpdates();

        // Assert
        InOrder order = inOrder(linkUpdateService, healthchecksClient);
        order.verify(linkUpdateService).checkForUpdates();
        order.verify(healthchecksClient).pingScrapper();
    }

    @Test
    void shouldNotPingScrapperWhenUpdateCycleFails() {
        // Arrange
        LinkUpdateService linkUpdateService = mock(LinkUpdateService.class);
        NotificationService notificationService = mock(NotificationService.class);
        HealthchecksClient healthchecksClient = mock(HealthchecksClient.class);
        UpdateScheduler scheduler = new UpdateScheduler(linkUpdateService, notificationService, healthchecksClient);
        doThrow(new DataAccessResourceFailureException("database unavailable"))
                .when(linkUpdateService)
                .checkForUpdates();

        // Act & Assert
        assertThatThrownBy(scheduler::checkUpdates).isInstanceOf(DataAccessResourceFailureException.class);
        verify(healthchecksClient, never()).pingScrapper();
    }
}
