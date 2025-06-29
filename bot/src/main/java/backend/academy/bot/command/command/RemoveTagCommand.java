package backend.academy.bot.command.command;

import backend.academy.bot.command.BotCommand;
import backend.academy.bot.dialog.DialogService;
import backend.academy.bot.dialog.DialogType;
import backend.academy.bot.service.CommandService;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import org.springframework.stereotype.Component;

@BotCommand(command = "/removetag", description = "Удалить тег у отслеживаемой ссылки.")
@Component
public class RemoveTagCommand extends AbstractTagCommand {
    private static final TagCommandConfig CONFIG = new TagCommandConfig(
            "/removetag",
            DialogType.REMOVE_TAG,
            "Использование: /removetag",
            "Ошибка. Пожалуйста, начните с /removetag",
            "Укажите ссылку для удаления тега.",
            "Укажите тег для удаления.",
            "Ссылка %s некорректна",
            "Введите один тег для удаления у ссылки.",
            "Тег удален успешно.",
            "Ошибка! Тег не удален.",
            "Произошла ошибка при удалении тега");

    public RemoveTagCommand(CommandService commandService, DialogService dialogService) {
        super(commandService, dialogService, CONFIG);
    }

    @Override
    protected void performOperation(Long chatId, String url, String tag, TelegramBot bot) {
        commandService
                .removeTagFromTrackedLink(chatId, url, tag)
                .subscribe(
                        success -> bot.execute(new SendMessage(chatId, success ? CONFIG.success() : CONFIG.failure())),
                        error -> bot.execute(new SendMessage(chatId, CONFIG.error())));
    }
}
