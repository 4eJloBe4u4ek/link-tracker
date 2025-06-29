package backend.academy.bot.command.command;

import backend.academy.bot.command.BotCommand;
import backend.academy.bot.dialog.DialogService;
import backend.academy.bot.dialog.DialogType;
import backend.academy.bot.service.CommandService;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import org.springframework.stereotype.Component;

@BotCommand(BotCommandInfo.REMOVE_FILTER)
@Component
public class RemoveFilterCommand extends AbstractFilterCommand {
    private static final FilterCommandConfig CONFIG = new FilterCommandConfig(
            BotCommandInfo.REMOVE_FILTER.commandName(),
            DialogType.REMOVE_FILTER,
            "Использование: " + BotCommandInfo.REMOVE_FILTER.commandName(),
            "Ошибка. Пожалуйста, начните с " + BotCommandInfo.REMOVE_FILTER.commandName(),
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
