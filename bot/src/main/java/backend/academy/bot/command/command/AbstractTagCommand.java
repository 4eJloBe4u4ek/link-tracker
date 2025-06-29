package backend.academy.bot.command.command;

import backend.academy.bot.command.TelegramCommand;
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
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class AbstractTagCommand implements TelegramCommand {
    private static final String SPACE_SPLIT_REGEX = "\\s+";

    protected final CommandService commandService;
    protected final DialogService dialogService;
    protected final TagCommandConfig tagCommandConfig;

    @Override
    public void execute(Update update, TelegramBot bot) {
        Long chatId = update.message().chat().id();
        String message = update.message().text().trim();
        TrackingContext trackingContext = dialogService.getDialog(chatId);

        if (message.startsWith(tagCommandConfig.command()) && trackingContext == null) {
            DialogUtils.startDialogAndNotify(
                    dialogService,
                    chatId,
                    bot,
                    tagCommandConfig.dialogType(),
                    TrackState.AWAITING_URL,
                    tagCommandConfig.usage(),
                    message,
                    tagCommandConfig.enterUrl());
        } else if (trackingContext != null) {
            switch (trackingContext.trackState()) {
                case AWAITING_URL -> processUrlInput(chatId, bot, trackingContext, message);
                case AWAITING_TAG -> processTagInput(chatId, bot, trackingContext, message);
                default -> {
                    bot.execute(new SendMessage(chatId, tagCommandConfig.unknownState()));
                    dialogService.endDialog(chatId);
                }
            }
        }
    }

    protected abstract void performOperation(Long chatId, String url, String tag, TelegramBot bot);

    private void processUrlInput(Long chatId, TelegramBot bot, TrackingContext trackingContext, String url) {
        if (!UrlChecker.isValidUrl(url)) {
            bot.execute(new SendMessage(chatId, String.format(tagCommandConfig.invalidUrl(), url)));
            return;
        }
        trackingContext.url(url);
        trackingContext.trackState(TrackState.AWAITING_TAG);
        bot.execute(new SendMessage(chatId, tagCommandConfig.enterTag()));
    }

    private void processTagInput(Long chatId, TelegramBot bot, TrackingContext trackingContext, String message) {
        trackingContext.tags(Arrays.asList(message.split(SPACE_SPLIT_REGEX)));
        if (trackingContext.tags().size() != 1) {
            bot.execute(new SendMessage(chatId, tagCommandConfig.invalidTagCount()));
            return;
        }
        trackingContext.trackState(TrackState.COMPLETED);

        performOperation(chatId, trackingContext.url(), trackingContext.tags().getFirst(), bot);

        dialogService.endDialog(chatId);
    }

    public record TagCommandConfig(
            String command,
            DialogType dialogType,
            String usage,
            String unknownState,
            String enterUrl,
            String enterTag,
            String invalidUrl,
            String invalidTagCount,
            String success,
            String failure,
            String error) {}
}
