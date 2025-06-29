package backend.academy.bot.command.command;

import backend.academy.bot.command.BotCommand;
import backend.academy.bot.dialog.DialogService;
import backend.academy.bot.dialog.DialogType;
import backend.academy.bot.service.CommandService;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import org.springframework.stereotype.Component;

@BotCommand(BotCommandInfo.ADD_TAG)
@Component
public class AddTagCommand extends AbstractTagCommand {
    private static final TagCommandConfig CONFIG = new TagCommandConfig(
            BotCommandInfo.ADD_TAG.commandName(),
            DialogType.ADD_TAG,
            "Использование: " + BotCommandInfo.ADD_TAG.commandName(),
            "Ошибка. Пожалуйста, начните с " + BotCommandInfo.ADD_TAG.commandName(),
            "Укажите ссылку для добавления тега.",
            "Укажите тег для добавления к ссылке.",
            "Ссылка %s некорректна",
            "Введите один тег для добавления к ссылке.",
            "Тег добавлен успешно.",
            "Ошибка! Тег не добавлен.",
            "Произошла ошибка при добавлении тега");

    public AddTagCommand(CommandService commandService, DialogService dialogService) {
        super(commandService, dialogService, CONFIG);
    }

    @Override
    protected void performOperation(Long chatId, String url, String tag, TelegramBot bot) {
        commandService
                .addTagToTrackedLink(chatId, url, tag)
                .subscribe(
                        success -> bot.execute(new SendMessage(chatId, success ? CONFIG.success() : CONFIG.failure())),
                        error -> bot.execute(new SendMessage(chatId, CONFIG.error())));
    }
}
