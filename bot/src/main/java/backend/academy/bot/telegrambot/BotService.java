package backend.academy.bot.telegrambot;

import backend.academy.bot.command.CommandHandler;
import backend.academy.bot.config.BotConfig;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.BotCommand;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.GetMe;
import com.pengrad.telegrambot.request.SetMyCommands;
import com.pengrad.telegrambot.response.GetMeResponse;
import jakarta.annotation.PostConstruct;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class BotService {
    @Getter
    private final TelegramBot bot;

    private final CommandHandler commandHandler;
    private final Clock clock;
    private final AtomicReference<Instant> lastPollingErrorAt = new AtomicReference<>();

    @Autowired
    public BotService(BotConfig config, CommandHandler commandHandler) {
        this(new TelegramBot(config.telegramToken()), commandHandler, Clock.systemUTC());
    }

    BotService(TelegramBot bot, CommandHandler commandHandler, Clock clock) {
        this.bot = bot;
        this.commandHandler = commandHandler;
        this.clock = clock;
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

        bot.setUpdatesListener(
                updates -> {
                    for (Update update : updates) {
                        if (update.message() != null && update.message().text() != null) {
                            commandHandler.handleCommand(update, bot);
                        }
                    }

                    return UpdatesListener.CONFIRMED_UPDATES_ALL;
                },
                exception -> {
                    lastPollingErrorAt.set(clock.instant());
                    log.warn("Telegram long polling failed: {}", exception.getClass().getSimpleName());
                });
    }

    public boolean telegramApiAvailable() {
        try {
            GetMeResponse response = bot.execute(new GetMe());
            return response != null && response.isOk();
        } catch (RuntimeException exception) {
            log.warn("Telegram getMe failed: {}", exception.getClass().getSimpleName());
            return false;
        }
    }

    public boolean hasRecentPollingError(Duration window) {
        Instant lastError = lastPollingErrorAt.get();
        if (lastError == null) {
            return false;
        }
        return clock.instant().isBefore(lastError.plus(window));
    }
}
