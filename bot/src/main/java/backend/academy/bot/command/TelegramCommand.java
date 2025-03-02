package backend.academy.bot.command;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;

public interface TelegramCommand {
    void execute(Update update, TelegramBot bot);
}
