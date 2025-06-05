package backend.academy.bot.service;

import backend.academy.bot.exception.LinkUpdateException;
import backend.academy.bot.telegrambot.BotService;
import backend.academy.shared.dto.LinkUpdate;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateService {
    private final BotService botService;

    public void processUpdate(LinkUpdate update) {
        if (update == null || update.tgChatIds().isEmpty()) {
            log.warn("Received invalid link update: empty chat list");
            throw new LinkUpdateException("Список чатов для обновления пуст");
        }

        for (Long chatId : update.tgChatIds()) {
            botService
                    .bot()
                    .execute(new SendMessage(
                            chatId, "Новое обновление!\nURL: " + update.url() + "\n" + update.description()));
        }

        log.atInfo()
                .setMessage("Sent update to all chats")
                .addKeyValue("url", update.url())
                .log();
    }
}
