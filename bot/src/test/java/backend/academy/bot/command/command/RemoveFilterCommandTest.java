package backend.academy.bot.command.command;

import static backend.academy.bot.TestData.CMD_REMOVE_FILTER;
import static backend.academy.bot.TestData.EXTRA_ARGUMENT;
import static backend.academy.bot.TestData.INVALID_FILTER_COUNT;
import static backend.academy.bot.TestData.REMOVE_FILTER_ERROR_FILTER_COUNT;
import static backend.academy.bot.TestData.REMOVE_FILTER_ERROR_TAG_REMOVE;
import static backend.academy.bot.TestData.REMOVE_FILTER_PROMPT_FILTER_NAME;
import static backend.academy.bot.TestData.REMOVE_FILTER_PROMPT_FILTER_URL;
import static backend.academy.bot.TestData.REMOVE_FILTER_SUCCESS_FILTER_REMOVED;
import static backend.academy.bot.TestData.TELEGRAM_PARAM_CHAT_ID;
import static backend.academy.bot.TestData.TELEGRAM_PARAM_TEXT;
import static backend.academy.bot.TestData.TEST_CHAT_ID;
import static backend.academy.bot.TestData.TEST_URL;
import static backend.academy.bot.TestData.USAGE_REMOVE_FILTER;
import static backend.academy.bot.TestData.VALID_FILTER_COUNT;
import static org.assertj.core.api.Assertions.assertThat;
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

class RemoveFilterCommandTest {
    private TelegramBot bot;
    private CommandService commandService;
    private DialogService dialogService;
    private RemoveFilterCommand removeFilterCommand;
    private Update update;
    private Message message;

    @BeforeEach
    void setUp() {
        bot = mock(TelegramBot.class);
        commandService = mock(CommandService.class);
        dialogService = new DialogService();
        removeFilterCommand = new RemoveFilterCommand(commandService, dialogService);

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
        when(message.text()).thenReturn(CMD_REMOVE_FILTER + EXTRA_ARGUMENT);

        // Act
        removeFilterCommand.execute(update, bot);

        // Assert
        Mockito.verify(bot)
                .execute(argThat(
                        msg -> msg.getParameters().get(TELEGRAM_PARAM_CHAT_ID).equals(TEST_CHAT_ID)
                                && msg.getParameters().get(TELEGRAM_PARAM_TEXT).equals(USAGE_REMOVE_FILTER)));
    }

    @Test
    void shouldStartRemoveFilterDialog() {
        // Arrange
        when(message.text()).thenReturn(CMD_REMOVE_FILTER);
        assertThat(dialogService.getDialog(TEST_CHAT_ID)).isNull();

        // Act
        removeFilterCommand.execute(update, bot);

        // Assert
        TrackingContext context = dialogService.getDialog(TEST_CHAT_ID);
        assertThat(context).isNotNull();
        assertThat(context.dialogType()).isEqualTo(DialogType.REMOVE_FILTER);
        assertThat(context.trackState()).isEqualTo(TrackState.AWAITING_URL);
        Mockito.verify(bot)
                .execute(argThat(msg -> msg.getParameters()
                                .get(TELEGRAM_PARAM_CHAT_ID)
                                .equals(TEST_CHAT_ID)
                        && msg.getParameters().get(TELEGRAM_PARAM_TEXT).equals(REMOVE_FILTER_PROMPT_FILTER_URL)));
    }

    @Test
    void shouldProcessValidUrlAndPromptFilter() {
        // Arrange
        when(message.text()).thenReturn(CMD_REMOVE_FILTER);
        removeFilterCommand.execute(update, bot);
        when(message.text()).thenReturn(TEST_URL);

        // Act
        removeFilterCommand.execute(update, bot);

        // Assert
        TrackingContext ctx = dialogService.getDialog(TEST_CHAT_ID);
        assertThat(ctx.url()).isEqualTo(TEST_URL);
        assertThat(ctx.trackState()).isEqualTo(TrackState.AWAITING_FILTER);
        Mockito.verify(bot)
                .execute(argThat(msg -> msg.getParameters()
                                .get(TELEGRAM_PARAM_CHAT_ID)
                                .equals(TEST_CHAT_ID)
                        && msg.getParameters().get(TELEGRAM_PARAM_TEXT).equals(REMOVE_FILTER_PROMPT_FILTER_NAME)));
    }

    @Test
    void shouldProcessInvalidFilterCount() {
        // Arrange
        when(message.text()).thenReturn(CMD_REMOVE_FILTER);
        removeFilterCommand.execute(update, bot);
        when(message.text()).thenReturn(TEST_URL);
        removeFilterCommand.execute(update, bot);
        when(message.text()).thenReturn(INVALID_FILTER_COUNT);

        // Act
        removeFilterCommand.execute(update, bot);

        // Assert
        Mockito.verify(bot)
                .execute(argThat(msg -> msg.getParameters()
                                .get(TELEGRAM_PARAM_CHAT_ID)
                                .equals(TEST_CHAT_ID)
                        && msg.getParameters().get(TELEGRAM_PARAM_TEXT).equals(REMOVE_FILTER_ERROR_FILTER_COUNT)));
        TrackingContext context = dialogService.getDialog(TEST_CHAT_ID);
        assertThat(context).isNotNull();
        assertThat(context.trackState()).isEqualTo(TrackState.AWAITING_FILTER);
    }

    @Test
    void shouldProcessFilterInputAndCallRemoveFilterSuccessfully() {
        // Arrange
        when(message.text()).thenReturn(CMD_REMOVE_FILTER);
        removeFilterCommand.execute(update, bot);
        when(message.text()).thenReturn(TEST_URL);
        removeFilterCommand.execute(update, bot);
        when(message.text()).thenReturn(VALID_FILTER_COUNT);
        when(commandService.removeFilterFromTrackedLink(TEST_CHAT_ID, TEST_URL, VALID_FILTER_COUNT))
                .thenReturn(Mono.just(true));

        // Act
        removeFilterCommand.execute(update, bot);

        // Assert
        Mockito.verify(bot)
                .execute(argThat(msg -> msg.getParameters()
                                .get(TELEGRAM_PARAM_CHAT_ID)
                                .equals(TEST_CHAT_ID)
                        && msg.getParameters().get(TELEGRAM_PARAM_TEXT).equals(REMOVE_FILTER_SUCCESS_FILTER_REMOVED)));
        assertThat(dialogService.getDialog(TEST_CHAT_ID)).isNull();
    }

    @Test
    void shouldProcessFilterInputAndCallRemoveFilterFailure() {
        // Arrange
        when(message.text()).thenReturn(CMD_REMOVE_FILTER);
        removeFilterCommand.execute(update, bot);
        when(message.text()).thenReturn(TEST_URL);
        removeFilterCommand.execute(update, bot);
        when(message.text()).thenReturn(VALID_FILTER_COUNT);
        when(commandService.removeFilterFromTrackedLink(TEST_CHAT_ID, TEST_URL, VALID_FILTER_COUNT))
                .thenReturn(Mono.just(false));

        // Act
        removeFilterCommand.execute(update, bot);

        // Assert
        Mockito.verify(bot)
                .execute(argThat(msg -> msg.getParameters()
                                .get(TELEGRAM_PARAM_CHAT_ID)
                                .equals(TEST_CHAT_ID)
                        && msg.getParameters().get(TELEGRAM_PARAM_TEXT).equals(REMOVE_FILTER_ERROR_TAG_REMOVE)));
        assertThat(dialogService.getDialog(TEST_CHAT_ID)).isNull();
    }
}
