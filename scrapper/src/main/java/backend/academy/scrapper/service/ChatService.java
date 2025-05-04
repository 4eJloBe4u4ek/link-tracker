package backend.academy.scrapper.service;

import backend.academy.scrapper.repository.ChatOperationRepository;
import backend.academy.shared.dto.RegisterChatRequest;
import backend.academy.shared.dto.UpdateNotificationModeRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatOperationRepository chatOperationRepository;

    public void registerChat(Long chatId, RegisterChatRequest registerChatRequest) {
        chatOperationRepository.registerChat(chatId, registerChatRequest.mode(), registerChatRequest.digestTime());
        log.atInfo()
                .setMessage("Successfully registered chat")
                .addKeyValue("chatId", chatId)
                .log();
    }

    public void deleteChat(Long chatId) {
        chatOperationRepository.deleteChat(chatId);
        log.atInfo()
                .setMessage("Successfully deleted chat")
                .addKeyValue("chatId", chatId)
                .log();
    }

    public void updateNotificationMode(Long chatId, UpdateNotificationModeRequest updateNotificationModeRequest) {
        chatOperationRepository.updateNotificationMode(
                chatId, updateNotificationModeRequest.mode(), updateNotificationModeRequest.digestTime());
        log.atInfo()
                .setMessage("Successfully updated notification mode")
                .addKeyValue("chatId", chatId)
                .addKeyValue("mode", updateNotificationModeRequest.mode())
                .log();
    }
}
