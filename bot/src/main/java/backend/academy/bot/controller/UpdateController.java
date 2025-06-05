package backend.academy.bot.controller;

import backend.academy.bot.ratelimiter.RateLimited;
import backend.academy.bot.service.UpdateService;
import backend.academy.shared.api.ApiEndpoints;
import backend.academy.shared.dto.LinkUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class UpdateController {
    private final UpdateService updateService;

    @RateLimited
    @PostMapping(ApiEndpoints.UPDATES)
    public ResponseEntity<Void> updateLink(@RequestBody LinkUpdate update) {
        log.atInfo()
                .setMessage("Getting link update by http")
                .addKeyValue("url", update.url())
                .log();
        updateService.processUpdate(update);
        return ResponseEntity.ok().build();
    }
}
