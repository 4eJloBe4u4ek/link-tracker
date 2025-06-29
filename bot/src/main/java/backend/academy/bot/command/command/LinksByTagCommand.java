package backend.academy.bot.command.command;

import static backend.academy.bot.command.Utils.NEW_LINE;
import static backend.academy.bot.command.Utils.SPACE_SPLIT_REGEX;

import backend.academy.bot.command.BotCommand;
import backend.academy.bot.command.TelegramCommand;
import backend.academy.bot.dialog.DialogService;
import backend.academy.bot.dialog.DialogType;
import backend.academy.bot.dialog.DialogUtils;
import backend.academy.bot.dialog.TrackState;
import backend.academy.bot.dialog.TrackingContext;
import backend.academy.bot.service.CommandService;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@BotCommand(BotCommandInfo.LINKS_BY_TAG)
@Component
@RequiredArgsConstructor
public class LinksByTagCommand implements TelegramCommand {
    private static final String EMPTY_LIST = "Список отслеживаемых ссылок по тегу пуст.";
    private static final String USAGE_MESSAGE = "Использование: " + BotCommandInfo.LINKS_BY_TAG.commandName();
    private static final String LIST_HEADER = "Список отслеживаемых ссылок по тегу:";
    private static final String ERROR_MESSAGE = "Произошла ошибка при получении отслеживаемых ссылок по тегу.";
    private static final String ENTER_TAG = "Укажите тег для вывода отслеживаемых ссылок.";
    private static final String UNKNOWN_STATE =
            "Ошибка. Пожалуйста, начните с " + BotCommandInfo.LINKS_BY_TAG.commandName();
    private static final String INVALID_TAG_COUNT = "Введите один тег для вывода списка отслеживаемых ссылок.";

    private final CommandService commandService;
    private final DialogService dialogService;

    @Override
    public void execute(Update update, TelegramBot bot) {
        Long chatId = update.message().chat().id();
        String message = update.message().text().trim();
        TrackingContext trackingContext = dialogService.getDialog(chatId);

        if (message.startsWith(BotCommandInfo.LINKS_BY_TAG.commandName()) && trackingContext == null) {
            DialogUtils.startDialogAndNotify(
                    dialogService,
                    chatId,
                    bot,
                    DialogType.LINKS_BY_TAG,
                    TrackState.AWAITING_TAG,
                    USAGE_MESSAGE,
                    message,
                    ENTER_TAG);
        } else {
            handleTrackingState(bot, trackingContext, chatId, message);
        }
    }

    private void handleTrackingState(TelegramBot bot, TrackingContext trackingContext, Long chatId, String message) {
        switch (trackingContext.trackState()) {
            case AWAITING_TAG -> processTagInput(chatId, bot, trackingContext, message);
            default -> {
                bot.execute(new SendMessage(chatId, UNKNOWN_STATE));
                dialogService.endDialog(chatId);
            }
        }
    }

    private void processTagInput(Long chatId, TelegramBot bot, TrackingContext trackingContext, String message) {
        trackingContext.tags(Arrays.asList(message.split(SPACE_SPLIT_REGEX)));
        if (trackingContext.tags().size() != 1) {
            bot.execute(new SendMessage(chatId, INVALID_TAG_COUNT));
            return;
        }
        trackingContext.trackState(TrackState.COMPLETED);

        commandService
                .getTrackedLinksByTag(chatId, trackingContext.tags().getFirst())
                .subscribe(
                        links -> {
                            String answer = links.isEmpty()
                                    ? EMPTY_LIST
                                    : LIST_HEADER + NEW_LINE + String.join(NEW_LINE, links);
                            bot.execute(new SendMessage(chatId, answer));
                        },
                        error -> bot.execute(new SendMessage(chatId, ERROR_MESSAGE)));

        dialogService.endDialog(chatId);
    }
}
