package backend.academy.bot.command.command;

import static backend.academy.bot.command.Utils.NEW_LINE;
import static backend.academy.bot.command.Utils.SPACE_SPLIT_REGEX;

import backend.academy.bot.command.BotCommand;
import backend.academy.bot.command.TelegramCommand;
import backend.academy.bot.service.CommandService;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@BotCommand(BotCommandInfo.LIST)
@Component
@RequiredArgsConstructor
public class ListCommand implements TelegramCommand {
    private static final String EMPTY_LIST = "Список отслеживаемых ссылок пуст.";
    private static final String USAGE_MESSAGE = "Использование: " + BotCommandInfo.LIST.commandName();
    private static final String LIST_HEADER = "Список отслеживаемых ссылок:";
    private static final String ERROR_MESSAGE = "Произошла ошибка при получении отслеживаемых ссылок.";
    private final CommandService commandService;

    @Override
    public void execute(Update update, TelegramBot bot) {
        Long chatId = update.message().chat().id();
        String[] messageParts = update.message().text().trim().split(SPACE_SPLIT_REGEX);

        if (messageParts.length != 1) {
            bot.execute(new SendMessage(chatId, USAGE_MESSAGE));
            return;
        }

        commandService
                .getTrackedLinks(chatId)
                .subscribe(
                        links -> {
                            String answer = links.isEmpty()
                                    ? EMPTY_LIST
                                    : LIST_HEADER + NEW_LINE + String.join(NEW_LINE, links);
                            bot.execute(new SendMessage(chatId, answer));
                        },
                        error -> bot.execute(new SendMessage(chatId, ERROR_MESSAGE)));
    }
}
