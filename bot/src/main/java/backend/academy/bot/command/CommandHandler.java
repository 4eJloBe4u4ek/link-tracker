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
import lombok.Getter;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class CommandHandler {
    private static final String UNKNOWN_COMMAND =
            "Неизвестная команда. Используйте /help для просмотра списка доступных команд.";
    private final Map<String, TelegramCommand> botCommands = new HashMap<>();

    @Getter
    private final Map<String, String> commandDescriptions = new HashMap<>();

    private final DialogService dialogService;

    private final ApplicationContext context;

    public CommandHandler(ApplicationContext context, DialogService dialogService) {
        this.dialogService = dialogService;
        this.context = context;
    }

    @PostConstruct
    public void init() {
        registerCommands(context);
    }

    public void handleCommand(Update update, TelegramBot bot) {
        Long chatId = update.message().chat().id();
        TrackingContext dialog = dialogService.getDialog(chatId);
        if (dialog != null && dialog.trackState() != TrackState.COMPLETED) {
            switch (dialog.dialogType()) {
                case TRACK -> botCommands.get("/track").execute(update, bot);
                case UNTRACK -> botCommands.get("/untrack").execute(update, bot);
            }
            return;
        }

        String messageCommand = update.message().text().trim().split("\\s+")[0];
        TelegramCommand command = botCommands.get(messageCommand);
        if (command != null) {
            command.execute(update, bot);
        } else {
            bot.execute(new SendMessage(chatId, UNKNOWN_COMMAND));
        }
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
}
