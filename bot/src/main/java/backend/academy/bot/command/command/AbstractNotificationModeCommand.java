package backend.academy.bot.command.command;

import static backend.academy.bot.command.Utils.HH_MM_TIME_FORMATTER;

import backend.academy.bot.command.TelegramCommand;
import backend.academy.bot.dialog.DialogService;
import backend.academy.bot.dialog.DialogType;
import backend.academy.bot.dialog.DialogUtils;
import backend.academy.bot.dialog.TrackState;
import backend.academy.bot.dialog.TrackingContext;
import backend.academy.bot.service.CommandService;
import backend.academy.shared.dto.NotificationMode;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class AbstractNotificationModeCommand implements TelegramCommand {
    protected final CommandService commandService;
    protected final DialogService dialogService;
    protected final NotificationModeCommandConfig notificationModeCommandConfig;

    @Override
    public void execute(Update update, TelegramBot bot) {
        Long chatId = update.message().chat().id();
        String message = update.message().text().trim();
        TrackingContext trackingContext = dialogService.getDialog(chatId);

        if (message.startsWith(notificationModeCommandConfig.command()) && trackingContext == null) {
            DialogUtils.startDialogAndNotify(
                    dialogService,
                    chatId,
                    bot,
                    notificationModeCommandConfig.dialogType(),
                    TrackState.AWAITING_NOTIFICATION_MODE,
                    notificationModeCommandConfig.usage(),
                    message,
                    notificationModeCommandConfig.enterNotificationMode());
        } else {
            handleTrackingState(bot, trackingContext, chatId, message);
        }
    }

    private void handleTrackingState(TelegramBot bot, TrackingContext trackingContext, Long chatId, String message) {
        switch (trackingContext.trackState()) {
            case AWAITING_NOTIFICATION_MODE -> processNotificationModeInput(chatId, bot, trackingContext, message);
            case AWAITING_NOTIFICATION_TIME -> processNotificationTimeInput(chatId, bot, trackingContext, message);
            default -> {
                bot.execute(new SendMessage(chatId, notificationModeCommandConfig.unknownState()));
                dialogService.endDialog(chatId);
            }
        }
    }

    protected abstract void performOperation(Long chatId, TrackingContext trackingContext, TelegramBot bot);

    private void processNotificationModeInput(
            Long chatId, TelegramBot bot, TrackingContext trackingContext, String message) {
        switch (message) {
            case "1" -> {
                trackingContext.notificationMode(NotificationMode.IMMEDIATE);
                trackingContext.trackState(TrackState.COMPLETED);
                performOperation(chatId, trackingContext, bot);
                dialogService.endDialog(chatId);
            }
            case "2" -> {
                trackingContext.notificationMode(NotificationMode.DAILY_DIGEST);
                trackingContext.trackState(TrackState.AWAITING_NOTIFICATION_TIME);
                bot.execute(new SendMessage(chatId, notificationModeCommandConfig.enterTimeMessage()));
            }
            default -> bot.execute(new SendMessage(chatId, notificationModeCommandConfig.invalidChoiceMessage()));
        }
    }

    private void processNotificationTimeInput(
            Long chatId, TelegramBot bot, TrackingContext trackingContext, String message) {
        try {
            LocalTime time = LocalTime.parse(message, HH_MM_TIME_FORMATTER);
            trackingContext.digestTime(time);
            trackingContext.trackState(TrackState.COMPLETED);
            performOperation(chatId, trackingContext, bot);
            dialogService.endDialog(chatId);
        } catch (DateTimeParseException e) {
            bot.execute(new SendMessage(chatId, notificationModeCommandConfig.invalidTimeMessage()));
        }
    }

    public record NotificationModeCommandConfig(
            String command,
            DialogType dialogType,
            String usage,
            String unknownState,
            String success,
            String failure,
            String error,
            String enterNotificationMode,
            String invalidChoiceMessage,
            String enterTimeMessage,
            String invalidTimeMessage) {}
}
