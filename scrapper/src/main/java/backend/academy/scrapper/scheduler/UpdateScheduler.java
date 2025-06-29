package backend.academy.scrapper.scheduler;

import backend.academy.scrapper.scheduler.service.LinkUpdateService;
import backend.academy.scrapper.scheduler.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateScheduler {
    private final LinkUpdateService linkUpdateService;
    private final NotificationService notificationService;

    @Scheduled(fixedDelayString = "#{scheduler.interval()}")
    public void checkUpdates() {
        log.info("Checking updates...");
        linkUpdateService.checkForUpdates();
        log.info("Finished checking updates");
    }

    @Scheduled(cron = "0 * * * * *")
    public void sendDailyDigest() {
        log.info("Triggering digest scheduler");
        notificationService.sendDailyDigest();
    }
}
