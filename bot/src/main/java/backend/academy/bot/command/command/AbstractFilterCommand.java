package backend.academy.bot.command.command;

import static backend.academy.bot.command.Utils.SPACE_SPLIT_REGEX;

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
public abstract class AbstractFilterCommand implements TelegramCommand {
    protected final CommandService commandService;
    protected final DialogService dialogService;
    protected final FilterCommandConfig filterCommandConfig;

    @Override
    public void execute(Update update, TelegramBot bot) {
        Long chatId = update.message().chat().id();
        String message = update.message().text().trim();
        TrackingContext trackingContext = dialogService.getDialog(chatId);

        if (message.startsWith(filterCommandConfig.command()) && trackingContext == null) {
            DialogUtils.startDialogAndNotify(
                    dialogService,
                    chatId,
                    bot,
                    filterCommandConfig.dialogType(),
                    TrackState.AWAITING_URL,
                    filterCommandConfig.usage(),
                    message,
                    filterCommandConfig.enterUrl());
        } else if (trackingContext != null) {
            handleTrackingState(bot, trackingContext, chatId, message);
        }
    }

    protected abstract void performOperation(Long chatId, String url, String filter, TelegramBot bot);

    private void handleTrackingState(TelegramBot bot, TrackingContext trackingContext, Long chatId, String message) {
        switch (trackingContext.trackState()) {
            case AWAITING_URL -> processUrlInput(chatId, bot, trackingContext, message);
            case AWAITING_FILTER -> processFilterInput(chatId, bot, trackingContext, message);
            default -> {
                bot.execute(new SendMessage(chatId, filterCommandConfig.unknownState()));
                dialogService.endDialog(chatId);
            }
        }
    }

    private void processUrlInput(Long chatId, TelegramBot bot, TrackingContext trackingContext, String url) {
        if (!UrlChecker.isValidUrl(url)) {
            bot.execute(new SendMessage(chatId, String.format(filterCommandConfig.invalidUrl(), url)));
            return;
        }
        trackingContext.url(url);
        trackingContext.trackState(TrackState.AWAITING_FILTER);
        bot.execute(new SendMessage(chatId, filterCommandConfig.enterFilter()));
    }

    private void processFilterInput(Long chatId, TelegramBot bot, TrackingContext trackingContext, String message) {
        trackingContext.filters(Arrays.asList(message.split(SPACE_SPLIT_REGEX)));
        if (trackingContext.filters().size() != 1) {
            bot.execute(new SendMessage(chatId, filterCommandConfig.invalidFilterCount()));
            return;
        }
        trackingContext.trackState(TrackState.COMPLETED);

        performOperation(
                chatId, trackingContext.url(), trackingContext.filters().getFirst(), bot);

        dialogService.endDialog(chatId);
    }

    public record FilterCommandConfig(
            String command,
            DialogType dialogType,
            String usage,
            String unknownState,
            String enterUrl,
            String enterFilter,
            String invalidUrl,
            String invalidFilterCount,
            String success,
            String failure,
            String error) {}
}
