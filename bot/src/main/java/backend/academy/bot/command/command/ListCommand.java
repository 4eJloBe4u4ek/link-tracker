package backend.academy.bot.command.command;

import backend.academy.bot.command.BotCommand;
import backend.academy.bot.command.TelegramCommand;
import backend.academy.bot.service.CommandService;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.springframework.stereotype.Component;

@BotCommand(command = "/list", description = "Список отслеживаемых ссылок.")
@Component
public class ListCommand implements TelegramCommand {
    private static final String EMPTY_LIST = "Список отслеживаемых ссылок пуст.";
    private static final String USAGE_MESSAGE = "Использование: /list";
    private static final String LIST_HEADER = "Список отслеживаемых ссылок:";
    private static final String ERROR_MESSAGE = "Произошла ошибка при получении отслеживаемых ссылок.";
    private final CommandService commandService;

    public ListCommand(CommandService commandService) {
        this.commandService = commandService;
    }

    @Override
    public void execute(Update update, TelegramBot bot) {
        Long chatId = update.message().chat().id();
        String[] messageParts = update.message().text().trim().split("\\s+");

        if (messageParts.length != 1) {
            bot.execute(new SendMessage(chatId, USAGE_MESSAGE));
            return;
        }

        StringBuilder trackedLinks = new StringBuilder(LIST_HEADER).append("\n");
        commandService
                .getTrackedLinks(chatId)
                .subscribe(
                        links -> {
                            if (links.isEmpty()) {
                                bot.execute(new SendMessage(chatId, EMPTY_LIST));
                            } else {
                                for (String link : links) {
                                    trackedLinks.append(link).append("\n");
                                }
                                bot.execute(new SendMessage(chatId, trackedLinks.toString()));
                            }
                        },
                        error -> {
                            bot.execute(new SendMessage(chatId, ERROR_MESSAGE));
                        });
    }
}
