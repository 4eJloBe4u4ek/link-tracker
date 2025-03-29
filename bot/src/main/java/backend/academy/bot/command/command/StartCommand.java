package backend.academy.bot.command.command;

import backend.academy.bot.command.BotCommand;
import backend.academy.bot.command.TelegramCommand;
import backend.academy.bot.service.CommandService;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@BotCommand(command = "/start", description = "Регистрация в боте.")
@Component
@RequiredArgsConstructor
public class StartCommand implements TelegramCommand {
    private static final String USAGE_MESSAGE = "Использование: /start";
    private static final String SUCCESS_MESSAGE = "Привет! Я бот для отслеживания ссылок.";
    private static final String ALREADY_EXISTS_MESSAGE = "Чат уже существует!";
    private static final String ERROR_MESSAGE = "Произошла ошибка при регистрации чата.";
    private final CommandService commandService;

    @Override
    public void execute(Update update, TelegramBot bot) {
        Long chatId = update.message().chat().id();
        String[] messageParts = update.message().text().split("\\s+");

        if (messageParts.length != 1) {
            bot.execute(new SendMessage(chatId, USAGE_MESSAGE));
            return;
        }

        commandService
                .registerChat(chatId)
                .subscribe(
                        success -> {
                            if (success) {
                                bot.execute(new SendMessage(chatId, SUCCESS_MESSAGE));
                            } else {
                                bot.execute(new SendMessage(chatId, ALREADY_EXISTS_MESSAGE));
                            }
                        },
                        error -> {
                            bot.execute(new SendMessage(chatId, ERROR_MESSAGE));
                        });
    }
}
