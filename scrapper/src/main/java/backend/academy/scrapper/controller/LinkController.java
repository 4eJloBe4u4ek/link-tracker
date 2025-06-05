package backend.academy.scrapper.controller;

import backend.academy.scrapper.ratelimiter.RateLimited;
import backend.academy.scrapper.service.LinkService;
import backend.academy.shared.api.ApiEndpoints;
import backend.academy.shared.api.ApiHeaders;
import backend.academy.shared.api.ApiParams;
import backend.academy.shared.dto.AddLinkRequest;
import backend.academy.shared.dto.LinkResponse;
import backend.academy.shared.dto.ListLinksResponse;
import backend.academy.shared.dto.RemoveLinkRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class LinkController {
    private final LinkService linkService;

    @RateLimited
    @GetMapping(ApiEndpoints.LINKS_BY_CHAT)
    public ResponseEntity<ListLinksResponse> getLinksByChat(@RequestHeader(ApiHeaders.TG_CHAT_ID) Long chatId) {
        log.atInfo()
                .setMessage("Getting links by chat")
                .addKeyValue("chatId", chatId)
                .log();
        return ResponseEntity.status(HttpStatus.OK).body(linkService.getLinksByChat(chatId));
    }

    @RateLimited
    @PostMapping(ApiEndpoints.LINKS_ADD)
    public ResponseEntity<LinkResponse> addLink(
            @RequestHeader(ApiHeaders.TG_CHAT_ID) Long chatId, @RequestBody AddLinkRequest addLinkRequest) {
        log.atInfo()
                .setMessage("Adding link")
                .addKeyValue("chatId", chatId)
                .addKeyValue("link", addLinkRequest.link())
                .log();
        return ResponseEntity.status(HttpStatus.OK).body(linkService.addLink(chatId, addLinkRequest));
    }

    @RateLimited
    @DeleteMapping(ApiEndpoints.LINKS_DELETE)
    public ResponseEntity<LinkResponse> deleteLink(
            @RequestHeader(ApiHeaders.TG_CHAT_ID) Long chatId, @RequestBody RemoveLinkRequest removeLinkRequest) {
        log.atInfo()
                .setMessage("Deleting link")
                .addKeyValue("chatId", chatId)
                .addKeyValue("link", removeLinkRequest.link())
                .log();
        return ResponseEntity.status(HttpStatus.OK).body(linkService.removeLink(chatId, removeLinkRequest));
    }

    @RateLimited
    @GetMapping(ApiEndpoints.LINKS_BY_TAG)
    public ResponseEntity<ListLinksResponse> getLinksByChatAndTag(
            @RequestHeader(ApiHeaders.TG_CHAT_ID) Long chatId, @RequestParam(ApiParams.TAG) String tag) {
        log.atInfo()
                .setMessage("Getting links by chat and tag")
                .addKeyValue("chatId", chatId)
                .addKeyValue("tag", tag)
                .log();
        return ResponseEntity.status(HttpStatus.OK).body(linkService.getLinksByChatAndTag(chatId, tag));
    }
}
