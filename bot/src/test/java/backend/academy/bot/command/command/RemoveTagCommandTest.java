package backend.academy.bot.command.command;

import static backend.academy.bot.TestData.CMD_REMOVE_TAG;
import static backend.academy.bot.TestData.EXTRA_ARGUMENT;
import static backend.academy.bot.TestData.INVALID_TAG_COUNT;
import static backend.academy.bot.TestData.REMOVE_TAG_ERROR_TAG_COUNT;
import static backend.academy.bot.TestData.REMOVE_TAG_ERROR_TAG_REMOVE;
import static backend.academy.bot.TestData.REMOVE_TAG_PROMPT_TAG_NAME;
import static backend.academy.bot.TestData.REMOVE_TAG_PROMPT_TAG_URL;
import static backend.academy.bot.TestData.REMOVE_TAG_SUCCESS_TAG_REMOVED;
import static backend.academy.bot.TestData.TELEGRAM_PARAM_CHAT_ID;
import static backend.academy.bot.TestData.TELEGRAM_PARAM_TEXT;
import static backend.academy.bot.TestData.TEST_CHAT_ID;
import static backend.academy.bot.TestData.TEST_URL;
import static backend.academy.bot.TestData.USAGE_REMOVE_TAG;
import static backend.academy.bot.TestData.VALID_TAG_COUNT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.bot.dialog.DialogService;
import backend.academy.bot.dialog.DialogType;
import backend.academy.bot.dialog.TrackState;
import backend.academy.bot.dialog.TrackingContext;
import backend.academy.bot.service.CommandService;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;

class RemoveTagCommandTest {
    private TelegramBot bot;
    private CommandService commandService;
    private DialogService dialogService;
    private RemoveTagCommand removeTagCommand;
    private Update update;
    private Message message;

    @BeforeEach
    void setUp() {
        bot = mock(TelegramBot.class);
        commandService = mock(CommandService.class);
        dialogService = new DialogService();
        removeTagCommand = new RemoveTagCommand(commandService, dialogService);

        update = mock(Update.class);
        message = mock(Message.class);
        Chat chat = mock(Chat.class);

        when(update.message()).thenReturn(message);
        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(TEST_CHAT_ID);
    }

    @Test
    void shouldReturnUsageErrorForExtraArguments() {
        // Arrange
        when(message.text()).thenReturn(CMD_REMOVE_TAG + EXTRA_ARGUMENT);

        // Act
        removeTagCommand.execute(update, bot);

        // Assert
        verify(bot)
                .execute(argThat(
                        msg -> msg.getParameters().get(TELEGRAM_PARAM_CHAT_ID).equals(TEST_CHAT_ID)
                                && msg.getParameters().get(TELEGRAM_PARAM_TEXT).equals(USAGE_REMOVE_TAG)));
    }

    @Test
    void shouldStartRemoveTagDialog() {
        // Arrange
        when(message.text()).thenReturn(CMD_REMOVE_TAG);

        // Act
        removeTagCommand.execute(update, bot);

        // Assert
        TrackingContext context = dialogService.getDialog(TEST_CHAT_ID);
        assertThat(context).isNotNull();
        assertThat(context.dialogType()).isEqualTo(DialogType.REMOVE_TAG);
        assertThat(context.trackState()).isEqualTo(TrackState.AWAITING_URL);
        verify(bot)
                .execute(argThat(
                        msg -> msg.getParameters().get(TELEGRAM_PARAM_CHAT_ID).equals(TEST_CHAT_ID)
                                && msg.getParameters().get(TELEGRAM_PARAM_TEXT).equals(REMOVE_TAG_PROMPT_TAG_URL)));
    }

    @Test
    void shouldProcessValidUrlAndPromptTag() {
        // Arrange
        when(message.text()).thenReturn(CMD_REMOVE_TAG);
        removeTagCommand.execute(update, bot);
        when(message.text()).thenReturn(TEST_URL);

        // Act
        removeTagCommand.execute(update, bot);

        // Assert
        TrackingContext ctx = dialogService.getDialog(TEST_CHAT_ID);
        assertThat(ctx.url()).isEqualTo(TEST_URL);
        assertThat(ctx.trackState()).isEqualTo(TrackState.AWAITING_TAG);
        verify(bot)
                .execute(argThat(
                        msg -> msg.getParameters().get(TELEGRAM_PARAM_CHAT_ID).equals(TEST_CHAT_ID)
                                && msg.getParameters().get(TELEGRAM_PARAM_TEXT).equals(REMOVE_TAG_PROMPT_TAG_NAME)));
    }

    @Test
    void shouldProcessInvalidTagCount() {
        // Arrange
        when(message.text()).thenReturn(CMD_REMOVE_TAG);
        removeTagCommand.execute(update, bot);
        when(message.text()).thenReturn(TEST_URL);
        removeTagCommand.execute(update, bot);
        when(message.text()).thenReturn(INVALID_TAG_COUNT);

        // Act
        removeTagCommand.execute(update, bot);

        // Assert
        verify(bot)
                .execute(argThat(
                        msg -> msg.getParameters().get(TELEGRAM_PARAM_CHAT_ID).equals(TEST_CHAT_ID)
                                && msg.getParameters().get(TELEGRAM_PARAM_TEXT).equals(REMOVE_TAG_ERROR_TAG_COUNT)));
        TrackingContext context = dialogService.getDialog(TEST_CHAT_ID);
        assertThat(context).isNotNull();
        assertThat(context.trackState()).isEqualTo(TrackState.AWAITING_TAG);
    }

    @Test
    void shouldProcessTagInputAndCallRemoveTagSuccessfully() {
        // Arrange
        when(message.text()).thenReturn(CMD_REMOVE_TAG);
        removeTagCommand.execute(update, bot);
        when(message.text()).thenReturn(TEST_URL);
        removeTagCommand.execute(update, bot);
        when(message.text()).thenReturn(VALID_TAG_COUNT);
        when(commandService.removeTagFromTrackedLink(TEST_CHAT_ID, TEST_URL, VALID_TAG_COUNT))
                .thenReturn(Mono.just(true));

        // Act
        removeTagCommand.execute(update, bot);

        // Assert
        verify(bot)
                .execute(argThat(msg -> msg.getParameters()
                                .get(TELEGRAM_PARAM_CHAT_ID)
                                .equals(TEST_CHAT_ID)
                        && msg.getParameters().get(TELEGRAM_PARAM_TEXT).equals(REMOVE_TAG_SUCCESS_TAG_REMOVED)));
        assertThat(dialogService.getDialog(TEST_CHAT_ID)).isNull();
    }

    @Test
    void shouldProcessTagInputAndCallRemoveTagFailure() {
        // Arrange
        when(message.text()).thenReturn(CMD_REMOVE_TAG);
        removeTagCommand.execute(update, bot);
        when(message.text()).thenReturn(TEST_URL);
        removeTagCommand.execute(update, bot);
        when(message.text()).thenReturn(VALID_TAG_COUNT);
        when(commandService.removeTagFromTrackedLink(TEST_CHAT_ID, TEST_URL, VALID_TAG_COUNT))
                .thenReturn(Mono.just(false));

        // Act
        removeTagCommand.execute(update, bot);

        // Assert
        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(bot, times(3)).execute(captor.capture());
        List<SendMessage> calls = captor.getAllValues();
        SendMessage third = calls.get(2);
        assertThat(third.getParameters().get(TELEGRAM_PARAM_TEXT)).isEqualTo(REMOVE_TAG_ERROR_TAG_REMOVE);
        assertThat(dialogService.getDialog(TEST_CHAT_ID)).isNull();
    }
}
