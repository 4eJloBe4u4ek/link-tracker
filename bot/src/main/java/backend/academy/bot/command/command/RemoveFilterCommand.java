package backend.academy.bot.command.command;

import backend.academy.bot.command.BotCommand;
import backend.academy.bot.dialog.DialogService;
import backend.academy.bot.dialog.DialogType;
import backend.academy.bot.service.CommandService;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import org.springframework.stereotype.Component;

@BotCommand(command = "/removefilter", description = "Удалить фильтр у отслеживаемой ссылки.")
@Component
public class RemoveFilterCommand extends AbstractFilterCommand {
    private static final FilterCommandConfig CONFIG = new FilterCommandConfig(
            "/removefilter",
            DialogType.REMOVE_FILTER,
            "Использование: /removefilter",
            "Ошибка. Пожалуйста, начните с /removefilter",
            "Укажите ссылку для удаления фильтра.",
            "Укажите фильтр для удаления.",
            "Ссылка %s некорректна",
            "Введите один фильтр для удаления у ссылки.",
            "Фильтр удален успешно.",
            "Ошибка! Фильтр не удален.",
            "Произошла ошибка при удалении фильтра");

    public RemoveFilterCommand(CommandService commandService, DialogService dialogService) {
        super(commandService, dialogService, CONFIG);
    }

    @Override
    protected void performOperation(Long chatId, String url, String filter, TelegramBot bot) {
        commandService
                .removeFilterFromTrackedLink(chatId, url, filter)
                .subscribe(
                        success -> bot.execute(new SendMessage(chatId, success ? CONFIG.success() : CONFIG.failure())),
                        error -> bot.execute(new SendMessage(chatId, CONFIG.error())));
    }
}
