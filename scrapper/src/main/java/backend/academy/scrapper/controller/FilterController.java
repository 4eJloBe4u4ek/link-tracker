package backend.academy.scrapper.controller;

import backend.academy.scrapper.ratelimiter.RateLimited;
import backend.academy.scrapper.service.FilterService;
import backend.academy.shared.dto.AddFilterRequest;
import backend.academy.shared.dto.RemoveFilterRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class FilterController {
    private final FilterService filterService;

    @RateLimited
    @PostMapping("/filters/add")
    public ResponseEntity<Void> addFilterToLink(
            @RequestHeader("Tg-Chat-Id") Long chatId, @RequestBody AddFilterRequest addFilterRequest) {
        log.atInfo()
                .setMessage("Adding filter to link")
                .addKeyValue("chatId", chatId)
                .addKeyValue("url", addFilterRequest.url())
                .addKeyValue("filter", addFilterRequest.filter())
                .log();
        filterService.addFilterToLink(chatId, addFilterRequest);
        return ResponseEntity.ok().build();
    }

    @RateLimited
    @DeleteMapping("/filters/remove")
    public ResponseEntity<Void> removeFilterFromLink(
            @RequestHeader("Tg-Chat-Id") Long chatId, @RequestBody RemoveFilterRequest removeFilterRequest) {
        log.atInfo()
                .setMessage("Removing filter from link")
                .addKeyValue("chatId", chatId)
                .addKeyValue("url", removeFilterRequest.url())
                .addKeyValue("filter", removeFilterRequest.filter())
                .log();
        filterService.removeFilterFromLink(chatId, removeFilterRequest);
        return ResponseEntity.ok().build();
    }
}
