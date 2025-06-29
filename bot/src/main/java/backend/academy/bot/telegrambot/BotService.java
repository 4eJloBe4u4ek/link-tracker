package backend.academy.bot.telegrambot;

import backend.academy.bot.command.CommandHandler;
import backend.academy.bot.config.BotConfig;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.BotCommand;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SetMyCommands;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class BotService {
    @Getter
    private final TelegramBot bot;

    private final CommandHandler commandHandler;

    public BotService(BotConfig config, CommandHandler commandHandler) {
        this.bot = new TelegramBot(config.telegramToken());
        this.commandHandler = commandHandler;
    }

    @PostConstruct
    public void start() {
        log.info("Starting Telegram bot");

        Map<String, String> commandsDescription = commandHandler.commandDescriptions();
        List<BotCommand> botCommands = new ArrayList<>();
        for (Map.Entry<String, String> entry : commandsDescription.entrySet()) {
            botCommands.add(new BotCommand(entry.getKey(), entry.getValue()));
        }
        bot.execute(new SetMyCommands(botCommands.toArray(new BotCommand[0])));

        bot.setUpdatesListener(updates -> {
            for (Update update : updates) {
                if (update.message() != null && update.message().text() != null) {
                    commandHandler.handleCommand(update, bot);
                }
            }

            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });
    }
}
