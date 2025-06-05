package backend.academy.bot.command.command;

import backend.academy.bot.command.BotCommand;
import backend.academy.bot.dialog.DialogService;
import backend.academy.bot.dialog.DialogType;
import backend.academy.bot.service.CommandService;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import org.springframework.stereotype.Component;

@BotCommand(BotCommandInfo.ADD_FILTER)
@Component
public class AddFilterCommand extends AbstractFilterCommand {
    private static final FilterCommandConfig CONFIG = new FilterCommandConfig(
            BotCommandInfo.ADD_FILTER.commandName(),
            DialogType.ADD_FILTER,
            "Использование: " + BotCommandInfo.ADD_FILTER.commandName(),
            "Ошибка. Пожалуйста, начните с " + BotCommandInfo.ADD_FILTER.commandName(),
            "Укажите ссылку для добавления фильтра.",
            "Укажите фильтр для добавления к ссылке.",
            "Ссылка %s некорректна",
            "Введите один фильтр для добавления к ссылке.",
            "Фильтр добавлен успешно.",
            "Ошибка! Фильтр не добавлен.",
            "Произошла ошибка при добавлении фильтра");

    public AddFilterCommand(CommandService commandService, DialogService dialogService) {
        super(commandService, dialogService, CONFIG);
    }

    @Override
    protected void performOperation(Long chatId, String url, String filter, TelegramBot bot) {
        commandService
                .addFilterToTrackedLink(chatId, url, filter)
                .subscribe(
                        success -> bot.execute(new SendMessage(chatId, success ? CONFIG.success() : CONFIG.failure())),
                        error -> bot.execute(new SendMessage(chatId, CONFIG.error())));
    }
}
