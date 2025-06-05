package backend.academy.scrapper.controller;

import backend.academy.scrapper.ratelimiter.RateLimited;
import backend.academy.scrapper.service.ChatService;
import backend.academy.shared.api.ApiEndpoints;
import backend.academy.shared.api.ApiParams;
import backend.academy.shared.dto.RegisterChatRequest;
import backend.academy.shared.dto.UpdateNotificationModeRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;

    @RateLimited
    @PostMapping(ApiEndpoints.TG_CHAT_BY_ID)
    public ResponseEntity<Void> registerChat(
            @PathVariable(ApiParams.ID) Long id, @RequestBody RegisterChatRequest registerChatRequest) {
        log.atInfo().setMessage("Registering chat").addKeyValue("chatId", id).log();
        chatService.registerChat(id, registerChatRequest);
        return ResponseEntity.ok().build();
    }

    @RateLimited
    @DeleteMapping(ApiEndpoints.TG_CHAT_BY_ID)
    public ResponseEntity<Void> deleteChat(@PathVariable(ApiParams.ID) Long id) {
        log.atInfo().setMessage("Deleting chat").addKeyValue("chatId", id).log();
        chatService.deleteChat(id);
        return ResponseEntity.ok().build();
    }

    @RateLimited
    @PatchMapping(ApiEndpoints.TG_CHAT_NOTIFICATION_BY_ID)
    public ResponseEntity<Void> updateNotificationMode(
            @PathVariable(ApiParams.ID) Long id,
            @RequestBody UpdateNotificationModeRequest updateNotificationModeRequest) {
        log.atInfo()
                .setMessage("Updating notification mode")
                .addKeyValue("chatId", id)
                .log();
        chatService.updateNotificationMode(id, updateNotificationModeRequest);
        return ResponseEntity.ok().build();
    }
}
