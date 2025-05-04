package backend.academy.bot.command.command;

import static backend.academy.bot.TestData.CMD_UNTRACK;
import static backend.academy.bot.TestData.EXTRA_ARGUMENT;
import static backend.academy.bot.TestData.TELEGRAM_PARAM_CHAT_ID;
import static backend.academy.bot.TestData.TELEGRAM_PARAM_TEXT;
import static backend.academy.bot.TestData.TEST_CHAT_ID;
import static backend.academy.bot.TestData.TEST_URL;
import static backend.academy.bot.TestData.UNTRACK_ERROR_URL;
import static backend.academy.bot.TestData.UNTRACK_PROMPT_URL;
import static backend.academy.bot.TestData.UNTRACK_SUCCESS_MESSAGE;
import static backend.academy.bot.TestData.USAGE_UNTRACK;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import backend.academy.bot.dialog.DialogService;
import backend.academy.bot.dialog.DialogType;
import backend.academy.bot.dialog.TrackState;
import backend.academy.bot.service.CommandService;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

class UntrackCommandTest {
    private TelegramBot bot;
    private CommandService commandService;
    private DialogService dialogService;
    private UntrackCommand untrackCommand;
    private Update update;
    private Message message;

    @BeforeEach
    void setUp() {
        bot = mock(TelegramBot.class);
        commandService = mock(CommandService.class);
        dialogService = new DialogService();
        untrackCommand = new UntrackCommand(commandService, dialogService);

        update = mock(Update.class);
        message = mock(Message.class);
        Chat chat = mock(Chat.class);

        when(update.message()).thenReturn(message);
        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(TEST_CHAT_ID);
    }

    @Test
    void shouldReturnUsageErrorForExtraArguments() {
        when(message.text()).thenReturn(CMD_UNTRACK + EXTRA_ARGUMENT);

        untrackCommand.execute(update, bot);

        Mockito.verify(bot)
                .execute(Mockito.argThat(
                        msg -> msg.getParameters().get(TELEGRAM_PARAM_CHAT_ID).equals(TEST_CHAT_ID)
                                && msg.getParameters().get(TELEGRAM_PARAM_TEXT).equals(USAGE_UNTRACK)));
    }

    @Test
    void shouldStartUntrackDialog() {
        when(message.text()).thenReturn(CMD_UNTRACK);

        untrackCommand.execute(update, bot);

        assertThat(dialogService.getDialog(TEST_CHAT_ID).dialogType()).isEqualTo(DialogType.UNTRACK);
        Mockito.verify(bot)
                .execute(Mockito.argThat(
                        msg -> msg.getParameters().get(TELEGRAM_PARAM_CHAT_ID).equals(TEST_CHAT_ID)
                                && msg.getParameters().get(TELEGRAM_PARAM_TEXT).equals(UNTRACK_PROMPT_URL)));
    }

    @Test
    void shouldUntrackValidUrlSuccessfully() {
        dialogService.startDialog(TEST_CHAT_ID, DialogType.UNTRACK);
        dialogService.getDialog(TEST_CHAT_ID).trackState(TrackState.AWAITING_URL);
        when(message.text()).thenReturn(TEST_URL);
        when(commandService.untrackLink(TEST_CHAT_ID, TEST_URL)).thenReturn(Mono.just(true));

        untrackCommand.execute(update, bot);

        Mockito.verify(bot)
                .execute(Mockito.argThat(
                        msg -> msg.getParameters().get(TELEGRAM_PARAM_CHAT_ID).equals(TEST_CHAT_ID)
                                && msg.getParameters()
                                        .get(TELEGRAM_PARAM_TEXT)
                                        .equals(String.format(UNTRACK_SUCCESS_MESSAGE, TEST_URL))));
        assertThat(dialogService.getDialog(TEST_CHAT_ID)).isNull();
    }

    @Test
    void shouldHandleUntrackFailure() {
        dialogService.startDialog(TEST_CHAT_ID, DialogType.UNTRACK);
        dialogService.getDialog(TEST_CHAT_ID).trackState(TrackState.AWAITING_URL);
        when(message.text()).thenReturn(TEST_URL);
        when(commandService.untrackLink(TEST_CHAT_ID, TEST_URL)).thenReturn(Mono.just(false));

        untrackCommand.execute(update, bot);

        Mockito.verify(bot)
                .execute(Mockito.argThat(
                        msg -> msg.getParameters().get(TELEGRAM_PARAM_CHAT_ID).equals(TEST_CHAT_ID)
                                && msg.getParameters().get(TELEGRAM_PARAM_TEXT).equals(UNTRACK_ERROR_URL + TEST_URL)));
        assertThat(dialogService.getDialog(TEST_CHAT_ID)).isNull();
    }
}
