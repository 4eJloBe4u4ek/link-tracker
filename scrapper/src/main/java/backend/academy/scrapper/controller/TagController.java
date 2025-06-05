package backend.academy.scrapper.controller;

import backend.academy.scrapper.ratelimiter.RateLimited;
import backend.academy.scrapper.service.TagService;
import backend.academy.shared.api.ApiEndpoints;
import backend.academy.shared.api.ApiHeaders;
import backend.academy.shared.dto.AddTagRequest;
import backend.academy.shared.dto.RemoveTagRequest;
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
public class TagController {
    private final TagService tagService;

    @RateLimited
    @PostMapping(ApiEndpoints.TAGS_ADD)
    public ResponseEntity<Void> addTagToLink(
            @RequestHeader(ApiHeaders.TG_CHAT_ID) Long chatId, @RequestBody AddTagRequest addTagRequest) {
        log.atInfo()
                .setMessage("Adding tag to link")
                .addKeyValue("chatId", chatId)
                .addKeyValue("url", addTagRequest.url())
                .addKeyValue("tag", addTagRequest.tag())
                .log();
        tagService.addTagToLink(chatId, addTagRequest);
        return ResponseEntity.ok().build();
    }

    @RateLimited
    @DeleteMapping(ApiEndpoints.TAGS_REMOVE)
    public ResponseEntity<Void> removeTagFromLink(
            @RequestHeader(ApiHeaders.TG_CHAT_ID) Long chatId, @RequestBody RemoveTagRequest removeTagRequest) {
        log.atInfo()
                .setMessage("Removing tag from link")
                .addKeyValue("chatId", chatId)
                .addKeyValue("url", removeTagRequest.url())
                .addKeyValue("tag", removeTagRequest.tag())
                .log();
        tagService.removeTagFromLink(chatId, removeTagRequest);
        return ResponseEntity.ok().build();
    }
}
