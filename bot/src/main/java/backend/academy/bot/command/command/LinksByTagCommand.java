package backend.academy.bot.command.command;

import backend.academy.bot.command.BotCommand;
import backend.academy.bot.command.TelegramCommand;
import backend.academy.bot.dialog.DialogService;
import backend.academy.bot.dialog.DialogType;
import backend.academy.bot.dialog.TrackState;
import backend.academy.bot.dialog.TrackingContext;
import backend.academy.bot.service.CommandService;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@BotCommand(command = "/linksbytag", description = "Список отслеживаемых ссылок по тегу.")
@Component
@RequiredArgsConstructor
public class LinksByTagCommand implements TelegramCommand {
    private static final String EMPTY_LIST = "Список отслеживаемых ссылок по тегу пуст.";
    private static final String USAGE_MESSAGE = "Использование: /linksbytag";
    private static final String LIST_HEADER = "Список отслеживаемых ссылок по тегу:";
    private static final String ERROR_MESSAGE = "Произошла ошибка при получении отслеживаемых ссылок по тегу.";
    private static final String ENTER_TAG = "Укажите тег для вывода отслеживаемых ссылок.";
    private static final String UNKNOWN_STATE = "Ошибка. Пожалуйста, начните с /linksbytag";
    private static final String INVALID_TAG_COUNT = "Введите один тег для вывода списка отслеживаемых ссылок.";

    private final CommandService commandService;
    private final DialogService dialogService;

    @Override
    public void execute(Update update, TelegramBot bot) {
        Long chatId = update.message().chat().id();
        String message = update.message().text().trim();
        TrackingContext trackingContext = dialogService.getDialog(chatId);

        if (message.startsWith("/linksbytag") && dialogService.getDialog(chatId) == null) {
            processStartLinksByTag(chatId, bot, message);
        } else {
            switch (trackingContext.trackState()) {
                case AWAITING_TAG -> processTagInput(chatId, bot, trackingContext, message);
                default -> {
                    bot.execute(new SendMessage(chatId, UNKNOWN_STATE));
                    dialogService.endDialog(chatId);
                }
            }
        }
    }

    private void processStartLinksByTag(Long chatId, TelegramBot bot, String message) {
        String[] messageParts = message.split("\\s+");
        if (messageParts.length != 1) {
            bot.execute(new SendMessage(chatId, USAGE_MESSAGE));
            return;
        }

        dialogService.startDialog(chatId, DialogType.LINKS_BY_TAG);
        dialogService.getDialog(chatId).trackState(TrackState.AWAITING_TAG);
        bot.execute(new SendMessage(chatId, ENTER_TAG));
    }

    private void processTagInput(Long chatId, TelegramBot bot, TrackingContext trackingContext, String message) {
        trackingContext.tags(Arrays.asList(message.split("\\s+")));
        if (trackingContext.tags().size() != 1) {
            bot.execute(new SendMessage(chatId, INVALID_TAG_COUNT));
            return;
        }
        trackingContext.trackState(TrackState.COMPLETED);

        StringBuilder trackedLinksByTag = new StringBuilder(LIST_HEADER).append("\n");
        commandService
                .getTrackedLinksByTag(chatId, trackingContext.tags().getFirst())
                .subscribe(
                        links -> {
                            if (links.isEmpty()) {
                                bot.execute(new SendMessage(chatId, EMPTY_LIST));
                            } else {
                                for (String link : links) {
                                    trackedLinksByTag.append(link).append("\n");
                                }
                                bot.execute(new SendMessage(chatId, trackedLinksByTag.toString()));
                            }
                        },
                        error -> {
                            bot.execute(new SendMessage(chatId, ERROR_MESSAGE));
                        });

        dialogService.endDialog(chatId);
    }
}
