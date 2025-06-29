package backend.academy.bot.command.command;

import static backend.academy.bot.command.Utils.SPACE_SPLIT_REGEX;

import backend.academy.bot.command.*;
import backend.academy.bot.dialog.DialogService;
import backend.academy.bot.dialog.DialogType;
import backend.academy.bot.dialog.DialogUtils;
import backend.academy.bot.dialog.TrackState;
import backend.academy.bot.dialog.TrackingContext;
import backend.academy.bot.service.CommandService;
import backend.academy.bot.utils.UrlChecker;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.Arrays;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@BotCommand(BotCommandInfo.TRACK)
@Component
@RequiredArgsConstructor
public class TrackCommand implements TelegramCommand {
    private static final String SKIP_COMMAND_NAME = "/skip";
    private static final String TAGS_MESSAGE = "Введите теги через пробел (опционально - " + SKIP_COMMAND_NAME + "):";
    private static final String FILTERS_MESSAGE =
            "Введите фильтры через пробел (опционально - " + SKIP_COMMAND_NAME + "):";
    private static final String USAGE_MESSAGE = "Использование: " + BotCommandInfo.TRACK.commandName();
    private static final String INVALID_URL_MESSAGE = "Ссылка %s некорректна";
    private static final String SUCCESS_MESSAGE = "Ссылка %s добавлена в отслеживание.";
    private static final String FAILURE_MESSAGE = "Ошибка! Ссылка %s не добавлена в отслеживание.";
    private static final String ERROR_MESSAGE = "Произошла ошибка при добавлении ссылки.";
    private static final String ENTER_URL_TO_TRACK = "Укажите ссылку для отслеживания.";
    private static final String UNKNOWN_STATE =
            "Ошибка. Пожалуйста, начните отслеживание с " + BotCommandInfo.TRACK.commandName();

    private final CommandService commandService;
    private final DialogService dialogService;

    @Override
    public void execute(Update update, TelegramBot bot) {
        Long chatId = update.message().chat().id();
        String message = update.message().text().trim();
        TrackingContext trackingContext = dialogService.getDialog(chatId);

        if (message.startsWith(BotCommandInfo.TRACK.commandName()) && trackingContext == null) {
            DialogUtils.startDialogAndNotify(
                    dialogService,
                    chatId,
                    bot,
                    DialogType.TRACK,
                    TrackState.AWAITING_URL,
                    USAGE_MESSAGE,
                    message,
                    ENTER_URL_TO_TRACK);
        } else {
            handleTrackingState(bot, trackingContext, chatId, message);
        }
    }

    private void handleTrackingState(TelegramBot bot, TrackingContext trackingContext, Long chatId, String message) {
        switch (trackingContext.trackState()) {
            case AWAITING_URL -> processUrlInput(chatId, bot, trackingContext, message);
            case AWAITING_TAGS -> processTagsInput(chatId, bot, trackingContext, message);
            case AWAITING_FILTERS -> processFiltersInput(chatId, bot, trackingContext, message);
            default -> {
                bot.execute(new SendMessage(chatId, UNKNOWN_STATE));
                dialogService.endDialog(chatId);
            }
        }
    }

    private void processUrlInput(Long chatId, TelegramBot bot, TrackingContext trackingContext, String url) {
        if (!UrlChecker.isValidUrl(url)) {
            bot.execute(new SendMessage(chatId, String.format(INVALID_URL_MESSAGE, url)));
            return;
        }
        trackingContext.url(url);
        trackingContext.trackState(TrackState.AWAITING_TAGS);
        bot.execute(new SendMessage(chatId, TAGS_MESSAGE));
    }

    private void processTagsInput(Long chatId, TelegramBot bot, TrackingContext trackingContext, String message) {
        if (message.isEmpty() || message.equalsIgnoreCase(SKIP_COMMAND_NAME)) {
            trackingContext.tags(Collections.emptyList());
        } else {
            trackingContext.tags(Arrays.asList(message.split(SPACE_SPLIT_REGEX)));
        }
        trackingContext.trackState(TrackState.AWAITING_FILTERS);
        bot.execute(new SendMessage(chatId, FILTERS_MESSAGE));
    }

    private void processFiltersInput(Long chatId, TelegramBot bot, TrackingContext trackingContext, String message) {
        if (message.isEmpty() || message.equalsIgnoreCase(SKIP_COMMAND_NAME)) {
            trackingContext.filters(Collections.emptyList());
        } else {
            trackingContext.filters(Arrays.asList(message.split(SPACE_SPLIT_REGEX)));
        }
        trackingContext.trackState(TrackState.COMPLETED);

        commandService
                .trackLink(chatId, trackingContext.url(), trackingContext.tags(), trackingContext.filters())
                .subscribe(
                        success -> bot.execute(new SendMessage(
                                chatId,
                                success
                                        ? String.format(SUCCESS_MESSAGE, trackingContext.url())
                                        : String.format(FAILURE_MESSAGE, trackingContext.url()))),
                        error -> bot.execute(new SendMessage(chatId, ERROR_MESSAGE)));

        dialogService.endDialog(chatId);
    }
}
