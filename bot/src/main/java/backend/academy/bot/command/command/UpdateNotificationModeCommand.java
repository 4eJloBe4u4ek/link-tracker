package backend.academy.bot.command.command;

import backend.academy.bot.command.BotCommand;
import backend.academy.bot.command.TelegramCommand;
import backend.academy.bot.dialog.DialogService;
import backend.academy.bot.dialog.DialogType;
import backend.academy.bot.dialog.TrackingContext;
import backend.academy.bot.service.CommandService;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import org.springframework.stereotype.Component;

@BotCommand(BotCommandInfo.SET_MODE)
@Component
public class UpdateNotificationModeCommand extends AbstractNotificationModeCommand implements TelegramCommand {
    private static final NotificationModeCommandConfig CONFIG = new NotificationModeCommandConfig(
            BotCommandInfo.SET_MODE.commandName(),
            DialogType.UPDATE_NOTIFICATION_MODE,
            "Использование: " + BotCommandInfo.SET_MODE.commandName(),
            "Ошибка. Пожалуйста, начните с " + BotCommandInfo.SET_MODE.commandName(),
            "Режим уведомлений обновлен",
            "Ошибка! Режим уведомлений не обновлен.",
            "Произошла ошибка при обновлении режима уведомлений.",
            "Выберите новый режим уведомлений:\n1. Сразу\n2. Дайджест раз в сутки",
            "Пожалуйста, введите 1 или 2.",
            "Введите время для получения дайджеста в формате HH:mm (например, 10:00)",
            "Неверный формат времени. Используйте HH:mm, например: 10:00");

    public UpdateNotificationModeCommand(CommandService commandService, DialogService dialogService) {
        super(commandService, dialogService, CONFIG);
    }

    @Override
    protected void performOperation(Long chatId, TrackingContext trackingContext, TelegramBot bot) {
        commandService
                .updateNotificationMode(chatId, trackingContext.notificationMode(), trackingContext.digestTime())
                .subscribe(
                        success -> bot.execute(new SendMessage(chatId, success ? CONFIG.success() : CONFIG.failure())),
                        error -> bot.execute(new SendMessage(chatId, CONFIG.error())));
    }
}
