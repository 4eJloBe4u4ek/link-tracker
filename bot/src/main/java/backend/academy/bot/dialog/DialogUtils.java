package backend.academy.bot.dialog;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.experimental.UtilityClass;

@UtilityClass
public class DialogUtils {
    private static final String SPACE_SPLIT_REGEX = "\\s+";

    public static void startDialogAndNotify(
            DialogService dialogService,
            Long chatId,
            TelegramBot bot,
            DialogType dialogType,
            TrackState initialState,
            String usageMessage,
            String commandText,
            String notifyMessage) {
        String[] messageParts = commandText.split(SPACE_SPLIT_REGEX);
        if (messageParts.length != 1) {
            bot.execute(new SendMessage(chatId, usageMessage));
            return;
        }
        dialogService.startDialog(chatId, dialogType);
        dialogService.getDialog(chatId).trackState(initialState);
        bot.execute(new SendMessage(chatId, notifyMessage));
    }
}
