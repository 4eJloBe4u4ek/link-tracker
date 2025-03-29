package backend.academy.bot.command.command;

import backend.academy.bot.command.BotCommand;
import backend.academy.bot.command.TelegramCommand;
import backend.academy.bot.dialog.DialogService;
import backend.academy.bot.dialog.DialogType;
import backend.academy.bot.dialog.TrackState;
import backend.academy.bot.dialog.TrackingContext;
import backend.academy.bot.service.CommandService;
import backend.academy.bot.utils.UrlChecker;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@BotCommand(command = "/addtag", description = "Добавление тега к отслеживаемой ссылке.")
@Component
@RequiredArgsConstructor
public class AddTagCommand implements TelegramCommand {
    private static final String USAGE_MESSAGE = "Использование: /addtag";
    private static final String ERROR_MESSAGE = "Произошла ошибка при добавление тега к отслеживаемой ссылке.";
    private static final String UNKNOWN_STATE = "Ошибка. Пожалуйста, начните с /addtag";
    private static final String ENTER_URL = "Укажите ссылку для добавления тега.";
    private static final String ENTER_TAG = "Укажите тег для добавления к ссылке.";
    private static final String INVALID_URL_MESSAGE = "Ссылка %s некорректна";
    private static final String INVALID_TAG_COUNT = "Введите один тег для добавления к ссылке.";
    private static final String SUCCESS_MESSAGE = "Тег добавлен успешно.";
    private static final String FAILURE_MESSAGE = "Ошибка! Тег не добавлен.";

    private final CommandService commandService;
    private final DialogService dialogService;

    @Override
    public void execute(Update update, TelegramBot bot) {
        Long chatId = update.message().chat().id();
        String message = update.message().text().trim();
        TrackingContext trackingContext = dialogService.getDialog(chatId);

        if (message.startsWith("/addtag") && dialogService.getDialog(chatId) == null) {
            processStartAddTag(chatId, bot, message);
        } else {
            switch (trackingContext.trackState()) {
                case AWAITING_URL -> processUrlInput(chatId, bot, trackingContext, message);
                case AWAITING_TAG -> processTagInput(chatId, bot, trackingContext, message);
                default -> {
                    bot.execute(new SendMessage(chatId, UNKNOWN_STATE));
                    dialogService.endDialog(chatId);
                }
            }
        }
    }

    private void processStartAddTag(Long chatId, TelegramBot bot, String message) {
        String[] messageParts = message.split("\\s+");
        if (messageParts.length != 1) {
            bot.execute(new SendMessage(chatId, USAGE_MESSAGE));
            return;
        }

        dialogService.startDialog(chatId, DialogType.ADD_TAG);
        dialogService.getDialog(chatId).trackState(TrackState.AWAITING_URL);
        bot.execute(new SendMessage(chatId, ENTER_URL));
    }

    private void processUrlInput(Long chatId, TelegramBot bot, TrackingContext trackingContext, String url) {
        if (!UrlChecker.isValidUrl(url)) {
            bot.execute(new SendMessage(chatId, String.format(INVALID_URL_MESSAGE, url)));
            return;
        }
        trackingContext.url(url);
        trackingContext.trackState(TrackState.AWAITING_TAG);
        bot.execute(new SendMessage(chatId, ENTER_TAG));
    }

    private void processTagInput(Long chatId, TelegramBot bot, TrackingContext trackingContext, String message) {
        trackingContext.tags(Arrays.asList(message.split("\\s+")));
        if (trackingContext.tags().size() != 1) {
            bot.execute(new SendMessage(chatId, INVALID_TAG_COUNT));
            return;
        }
        trackingContext.trackState(TrackState.COMPLETED);

        commandService
                .addTagToTrackedLink(
                        chatId, trackingContext.url(), trackingContext.tags().getFirst())
                .subscribe(
                        success -> {
                            if (success) {
                                bot.execute(new SendMessage(chatId, SUCCESS_MESSAGE));
                            } else {
                                bot.execute(new SendMessage(chatId, FAILURE_MESSAGE));
                            }
                        },
                        error -> {
                            bot.execute(new SendMessage(chatId, ERROR_MESSAGE));
                        });

        dialogService.endDialog(chatId);
    }
}
