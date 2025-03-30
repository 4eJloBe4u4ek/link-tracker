package backend.academy.bot.command.command;

import backend.academy.bot.command.BotCommand;
import backend.academy.bot.command.CommandHandler;
import backend.academy.bot.command.TelegramCommand;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@BotCommand(command = "/help", description = "Список доступных команд.")
@Component
@RequiredArgsConstructor
public class HelpCommand implements TelegramCommand {
    private static final String SPACE_SPLIT_REGEX = "\\s+";
    private static final String NEW_LINE = "\n";
    private static final String COMMAND_NAME = "/help";
    private static final String HELP_HEADER = "Доступные команды:";
    private static final String USAGE_MESSAGE = "Использование: " + COMMAND_NAME;
    private static final String SEPARATOR = " - ";
    private final ApplicationContext context;

    @Override
    public void execute(Update update, TelegramBot bot) {
        Long chatId = update.message().chat().id();
        String[] messageParts = update.message().text().trim().split(SPACE_SPLIT_REGEX);

        if (messageParts.length != 1) {
            bot.execute(new SendMessage(chatId, USAGE_MESSAGE));
            return;
        }

        CommandHandler commandHandler = context.getBean(CommandHandler.class);

        StringBuilder helpText = new StringBuilder(HELP_HEADER).append(NEW_LINE);
        commandHandler.commandDescriptions().forEach((command, description) -> helpText.append(command)
                .append(SEPARATOR)
                .append(description)
                .append(NEW_LINE));

        bot.execute(new SendMessage(chatId, helpText.toString()));
    }
}
