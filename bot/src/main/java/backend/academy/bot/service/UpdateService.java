package backend.academy.bot.service;

import backend.academy.bot.exception.LinkUpdateException;
import backend.academy.shared.dto.LinkUpdate;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import org.springframework.stereotype.Service;

@Service
public class UpdateService {
    private final TelegramBot bot;

    public UpdateService(TelegramBot bot) {
        this.bot = bot;
    }

    public void processUpdate(LinkUpdate update) {
        if (update == null || update.tgChatIds().isEmpty()) {
            throw new LinkUpdateException("Список чатов для обновления пуст");
        }

        for (Long chatId : update.tgChatIds()) {
            bot.execute(new SendMessage(
                    chatId, "Новое обновление!\nURL: " + update.url() + "\nОписание: " + update.description()));
        }
    }
}
