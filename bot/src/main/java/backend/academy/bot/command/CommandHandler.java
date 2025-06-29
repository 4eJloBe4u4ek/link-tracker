package backend.academy.bot.command;

import backend.academy.bot.dialog.DialogService;
import backend.academy.bot.dialog.TrackState;
import backend.academy.bot.dialog.TrackingContext;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommandHandler {
    private static final String SPACE_SPLIT_REGEX = "\\s+";
    private static final String UNKNOWN_COMMAND =
            "Неизвестная команда. Используйте /help для просмотра списка доступных команд.";
    private final Map<String, TelegramCommand> botCommands = new HashMap<>();

    @Getter
    private final Map<String, String> commandDescriptions = new HashMap<>();

    private final DialogService dialogService;

    private final ApplicationContext context;

    @PostConstruct
    public void init() {
        registerCommands(context);
    }

    public void handleCommand(Update update, TelegramBot bot) {
        Long chatId = update.message().chat().id();
        TrackingContext dialog = dialogService.getDialog(chatId);
        String messageCommand = update.message().text().trim().split(SPACE_SPLIT_REGEX)[0];

        log.atInfo()
                .setMessage("Received command")
                .addKeyValue("chatId", chatId)
                .addKeyValue("command", messageCommand)
                .log();

        if (dialog != null && dialog.trackState() != TrackState.COMPLETED) {
            executeDialogCommand(update, bot, chatId);
            return;
        }

        Optional.ofNullable(botCommands.get(messageCommand))
                .ifPresentOrElse(
                        command -> command.execute(update, bot),
                        () -> handleUnknownCommand(bot, chatId, messageCommand));
    }

    private void registerCommands(ApplicationContext context) {
        Map<String, Object> beans = context.getBeansWithAnnotation(BotCommand.class);
        for (Object bean : beans.values()) {
            if (bean instanceof TelegramCommand command) {
                BotCommand annotation = bean.getClass().getAnnotation(BotCommand.class);
                botCommands.put(annotation.command(), command);
                commandDescriptions.put(annotation.command(), annotation.description());
            }
        }
    }

    private void executeDialogCommand(Update update, TelegramBot bot, Long chatId) {
        String commandKey =
                switch (dialogService.getDialog(chatId).dialogType()) {
                    case TRACK -> "/track";
                    case UNTRACK -> "/untrack";
                    case LINKS_BY_TAG -> "/linksbytag";
                    case ADD_TAG -> "/addtag";
                    case REMOVE_TAG -> "/removetag";
                };

        Optional.ofNullable(botCommands.get(commandKey)).ifPresent(command -> command.execute(update, bot));
    }

    private void handleUnknownCommand(TelegramBot bot, Long chatId, String command) {
        bot.execute(new SendMessage(chatId, UNKNOWN_COMMAND));
        log.atInfo()
                .setMessage("Unknown command received")
                .addKeyValue("chatId", chatId)
                .addKeyValue("command", command)
                .log();
    }
}
