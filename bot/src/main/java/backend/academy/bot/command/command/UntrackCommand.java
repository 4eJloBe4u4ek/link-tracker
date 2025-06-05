package backend.academy.bot.command.command;

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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@BotCommand(BotCommandInfo.UNTRACK)
@Component
@RequiredArgsConstructor
public class UntrackCommand implements TelegramCommand {
    private static final String USAGE_MESSAGE = "Использование: " + BotCommandInfo.UNTRACK.commandName();
    private static final String INVALID_URL_MESSAGE = "Ссылка %s некорректна";
    private static final String SUCCESS_MESSAGE = "Ссылка %s больше не отслеживается";
    private static final String FAILURE_MESSAGE = "Ошибка! Невозможно прекратить отслеживание ссылки: %s";
    private static final String ERROR_MESSAGE = "Произошла ошибка при отмене отслеживания ссылки.";
    private static final String ENTER_URL_TO_UNTRACK = "Укажите ссылку для прекращения отслеживания.";
    private static final String UNKNOWN_STATE =
            "Ошибка. Пожалуйста, начните прекращение отслеживания с " + BotCommandInfo.UNTRACK.commandName();

    private final CommandService commandService;
    private final DialogService dialogService;

    @Override
    public void execute(Update update, TelegramBot bot) {
        Long chatId = update.message().chat().id();
        String message = update.message().text().trim();
        TrackingContext trackingContext = dialogService.getDialog(chatId);

        if (message.startsWith(BotCommandInfo.UNTRACK.commandName()) && trackingContext == null) {
            DialogUtils.startDialogAndNotify(
                    dialogService,
                    chatId,
                    bot,
                    DialogType.UNTRACK,
                    TrackState.AWAITING_URL,
                    USAGE_MESSAGE,
                    message,
                    ENTER_URL_TO_UNTRACK);
        } else {
            handleTrackingState(bot, trackingContext, chatId, message);
        }
    }

    private void handleTrackingState(TelegramBot bot, TrackingContext trackingContext, Long chatId, String message) {
        switch (trackingContext.trackState()) {
            case AWAITING_URL -> processUrlInput(chatId, bot, trackingContext, message);
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
        trackingContext.trackState(TrackState.COMPLETED);

        commandService
                .untrackLink(chatId, url)
                .subscribe(
                        success -> bot.execute(new SendMessage(
                                chatId,
                                success ? String.format(SUCCESS_MESSAGE, url) : String.format(FAILURE_MESSAGE, url))),
                        error -> bot.execute(new SendMessage(chatId, ERROR_MESSAGE)));

        dialogService.endDialog(chatId);
    }
}
