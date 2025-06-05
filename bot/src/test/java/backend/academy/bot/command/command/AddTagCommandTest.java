package backend.academy.bot.command.command;

import static backend.academy.bot.TestData.ADD_TAG_ERROR_TAG_ADD;
import static backend.academy.bot.TestData.ADD_TAG_ERROR_TAG_COUNT;
import static backend.academy.bot.TestData.ADD_TAG_PROMPT_TAG_NAME;
import static backend.academy.bot.TestData.ADD_TAG_PROMPT_TAG_URL;
import static backend.academy.bot.TestData.ADD_TAG_SUCCESS_TAG_ADDED;
import static backend.academy.bot.TestData.CMD_ADD_TAG;
import static backend.academy.bot.TestData.EXTRA_ARGUMENT;
import static backend.academy.bot.TestData.INVALID_TAG_COUNT;
import static backend.academy.bot.TestData.TELEGRAM_PARAM_CHAT_ID;
import static backend.academy.bot.TestData.TELEGRAM_PARAM_TEXT;
import static backend.academy.bot.TestData.TEST_CHAT_ID;
import static backend.academy.bot.TestData.TEST_URL;
import static backend.academy.bot.TestData.USAGE_ADD_TAG;
import static backend.academy.bot.TestData.VALID_TAG_COUNT;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

class AddTagCommandTest {
    private TelegramBot bot;
    private CommandService commandService;
    private DialogService dialogService;
    private AddTagCommand addTagCommand;
    private Update update;
    private Message message;

    @BeforeEach
    void setUp() {
        bot = mock(TelegramBot.class);
        commandService = mock(CommandService.class);
        dialogService = new DialogService();
        addTagCommand = new AddTagCommand(commandService, dialogService);

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
        when(message.text()).thenReturn(CMD_ADD_TAG + EXTRA_ARGUMENT);

        // Act
        addTagCommand.execute(update, bot);

        // Assert
        Mockito.verify(bot)
                .execute(argThat(
                        msg -> msg.getParameters().get(TELEGRAM_PARAM_CHAT_ID).equals(TEST_CHAT_ID)
                                && msg.getParameters().get(TELEGRAM_PARAM_TEXT).equals(USAGE_ADD_TAG)));
    }

    @Test
    void shouldStartAddTagDialog() {
        // Arrange
        when(message.text()).thenReturn(CMD_ADD_TAG);
        assertThat(dialogService.getDialog(TEST_CHAT_ID)).isNull();

        // Act
        addTagCommand.execute(update, bot);

        // Assert
        TrackingContext context = dialogService.getDialog(TEST_CHAT_ID);
        assertThat(context).isNotNull();
        assertThat(context.dialogType()).isEqualTo(DialogType.ADD_TAG);
        assertThat(context.trackState()).isEqualTo(TrackState.AWAITING_URL);
        Mockito.verify(bot)
                .execute(Mockito.argThat(
                        msg -> msg.getParameters().get(TELEGRAM_PARAM_CHAT_ID).equals(TEST_CHAT_ID)
                                && msg.getParameters().get(TELEGRAM_PARAM_TEXT).equals(ADD_TAG_PROMPT_TAG_URL)));
    }

    @Test
    void shouldProcessValidUrlSuccessfully() {
        // Arrange
        when(message.text()).thenReturn(CMD_ADD_TAG);
        addTagCommand.execute(update, bot);
        when(message.text()).thenReturn(TEST_URL);

        // Act
        addTagCommand.execute(update, bot);

        // Assert
        TrackingContext context = dialogService.getDialog(TEST_CHAT_ID);
        assertThat(context.url()).isEqualTo(TEST_URL);
        assertThat(context.trackState()).isEqualTo(TrackState.AWAITING_TAG);
        Mockito.verify(bot)
                .execute(Mockito.argThat(
                        msg -> msg.getParameters().get(TELEGRAM_PARAM_CHAT_ID).equals(TEST_CHAT_ID)
                                && msg.getParameters().get(TELEGRAM_PARAM_TEXT).equals(ADD_TAG_PROMPT_TAG_NAME)));
    }

    @Test
    void shouldProcessInvalidTagCount() {
        // Arrange
        when(message.text()).thenReturn(CMD_ADD_TAG);
        addTagCommand.execute(update, bot);
        when(message.text()).thenReturn(TEST_URL);
        addTagCommand.execute(update, bot);
        when(message.text()).thenReturn(INVALID_TAG_COUNT);

        // Act
        addTagCommand.execute(update, bot);

        // Assert
        Mockito.verify(bot)
                .execute(Mockito.argThat(
                        msg -> msg.getParameters().get(TELEGRAM_PARAM_CHAT_ID).equals(TEST_CHAT_ID)
                                && msg.getParameters().get(TELEGRAM_PARAM_TEXT).equals(ADD_TAG_ERROR_TAG_COUNT)));
        TrackingContext context = dialogService.getDialog(TEST_CHAT_ID);
        assertThat(context).isNotNull();
        assertThat(context.trackState()).isEqualTo(TrackState.AWAITING_TAG);
    }

    @Test
    void shouldProcessTagInputAndCallAddTagSuccessfully() {
        // Arrange
        when(message.text()).thenReturn(CMD_ADD_TAG);
        addTagCommand.execute(update, bot);
        when(message.text()).thenReturn(TEST_URL);
        addTagCommand.execute(update, bot);
        when(message.text()).thenReturn(VALID_TAG_COUNT);
        when(commandService.addTagToTrackedLink(TEST_CHAT_ID, TEST_URL, VALID_TAG_COUNT))
                .thenReturn(Mono.just(true));

        // Act
        addTagCommand.execute(update, bot);

        // Assert
        Mockito.verify(bot)
                .execute(Mockito.argThat(
                        msg -> msg.getParameters().get(TELEGRAM_PARAM_CHAT_ID).equals(TEST_CHAT_ID)
                                && msg.getParameters().get(TELEGRAM_PARAM_TEXT).equals(ADD_TAG_SUCCESS_TAG_ADDED)));
        assertThat(dialogService.getDialog(TEST_CHAT_ID)).isNull();
    }

    @Test
    void shouldProcessTagInputAndCallAddTagFailure() {
        // Arrange
        when(message.text()).thenReturn(CMD_ADD_TAG);
        addTagCommand.execute(update, bot);
        when(message.text()).thenReturn(TEST_URL);
        addTagCommand.execute(update, bot);
        when(message.text()).thenReturn(VALID_TAG_COUNT);
        when(commandService.addTagToTrackedLink(TEST_CHAT_ID, TEST_URL, VALID_TAG_COUNT))
                .thenReturn(Mono.just(false));

        // Act
        addTagCommand.execute(update, bot);

        // Assert
        Mockito.verify(bot)
                .execute(Mockito.argThat(
                        msg -> msg.getParameters().get(TELEGRAM_PARAM_CHAT_ID).equals(TEST_CHAT_ID)
                                && msg.getParameters().get(TELEGRAM_PARAM_TEXT).equals(ADD_TAG_ERROR_TAG_ADD)));
        assertThat(dialogService.getDialog(TEST_CHAT_ID)).isNull();
    }
}
