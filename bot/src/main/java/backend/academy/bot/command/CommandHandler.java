package backend.academy.bot.command;

import static backend.academy.bot.command.Utils.SPACE_SPLIT_REGEX;

import backend.academy.bot.dialog.DialogService;
import backend.academy.bot.dialog.TrackState;
import backend.academy.bot.dialog.TrackingContext;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CommandHandler {
    private static final String UNKNOWN_COMMAND =
            "Неизвестная команда. Используйте /help для просмотра списка доступных команд.";
    private static final String USER_MESSAGES_COUNTER_NAME = "custom_user_messages_total";
    private static final String USER_MESSAGES_COUNTER_DESCRIPTION = "Общее число входящих пользовательских сообщений";
    private final Map<String, TelegramCommand> botCommands = new HashMap<>();

    @Getter
    private final Map<String, String> commandDescriptions = new HashMap<>();

    private final DialogService dialogService;

    private final ApplicationContext context;

    private final Counter userMessagesCounter;

    public CommandHandler(DialogService dialogService, ApplicationContext context, MeterRegistry registry) {
        this.dialogService = dialogService;
        this.context = context;
        this.userMessagesCounter = Counter.builder(USER_MESSAGES_COUNTER_NAME)
                .description(USER_MESSAGES_COUNTER_DESCRIPTION)
                .register(registry);
    }

    @PostConstruct
    public void init() {
        registerCommands(context);
    }

    public void handleCommand(Update update, TelegramBot bot) {
        userMessagesCounter.increment();
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
                String commandName = annotation.value().commandName();
                String commandDescription = annotation.value().commandDescription();
                botCommands.put(commandName, command);
                commandDescriptions.put(commandName, commandDescription);
            }
        }
    }

    private void executeDialogCommand(Update update, TelegramBot bot, Long chatId) {
        String commandKey = dialogService.getDialog(chatId).dialogType().command();
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
