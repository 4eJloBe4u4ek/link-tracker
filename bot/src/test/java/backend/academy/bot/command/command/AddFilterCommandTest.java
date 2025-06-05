package backend.academy.bot.command.command;

import static backend.academy.bot.TestData.ADD_FILTER_ERROR_FILTER_ADD;
import static backend.academy.bot.TestData.ADD_FILTER_ERROR_FILTER_COUNT;
import static backend.academy.bot.TestData.ADD_FILTER_PROMPT_FILTER_NAME;
import static backend.academy.bot.TestData.ADD_FILTER_PROMPT_FILTER_URL;
import static backend.academy.bot.TestData.ADD_FILTER_SUCCESS_FILTER_ADDED;
import static backend.academy.bot.TestData.CMD_ADD_FILTER;
import static backend.academy.bot.TestData.EXTRA_ARGUMENT;
import static backend.academy.bot.TestData.INVALID_FILTER_COUNT;
import static backend.academy.bot.TestData.TELEGRAM_PARAM_CHAT_ID;
import static backend.academy.bot.TestData.TELEGRAM_PARAM_TEXT;
import static backend.academy.bot.TestData.TEST_CHAT_ID;
import static backend.academy.bot.TestData.TEST_URL;
import static backend.academy.bot.TestData.USAGE_ADD_FILTER;
import static backend.academy.bot.TestData.VALID_FILTER_COUNT;
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
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

class AddFilterCommandTest {
    private TelegramBot bot;
    private CommandService commandService;
    private DialogService dialogService;
    private AddFilterCommand addFilterCommand;
    private Update update;
    private Message message;

    @BeforeEach
    void setUp() {
        bot = mock(TelegramBot.class);
        commandService = mock(CommandService.class);
        dialogService = new DialogService();
        addFilterCommand = new AddFilterCommand(commandService, dialogService);

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
        when(message.text()).thenReturn(CMD_ADD_FILTER + EXTRA_ARGUMENT);

        // Act
        addFilterCommand.execute(update, bot);

        // Assert
        Mockito.verify(bot)
                .execute(argThat(
                        msg -> msg.getParameters().get(TELEGRAM_PARAM_CHAT_ID).equals(TEST_CHAT_ID)
                                && msg.getParameters().get(TELEGRAM_PARAM_TEXT).equals(USAGE_ADD_FILTER)));
    }

    @Test
    void shouldStartAddFilterDialog() {
        // Arrange
        when(message.text()).thenReturn(CMD_ADD_FILTER);
        AssertionsForClassTypes.assertThat(dialogService.getDialog(TEST_CHAT_ID))
                .isNull();

        // Act
        addFilterCommand.execute(update, bot);

        // Assert
        TrackingContext context = dialogService.getDialog(TEST_CHAT_ID);
        AssertionsForClassTypes.assertThat(context).isNotNull();
        AssertionsForClassTypes.assertThat(context.dialogType()).isEqualTo(DialogType.ADD_FILTER);
        AssertionsForClassTypes.assertThat(context.trackState()).isEqualTo(TrackState.AWAITING_URL);
        Mockito.verify(bot)
                .execute(Mockito.argThat(
                        msg -> msg.getParameters().get(TELEGRAM_PARAM_CHAT_ID).equals(TEST_CHAT_ID)
                                && msg.getParameters().get(TELEGRAM_PARAM_TEXT).equals(ADD_FILTER_PROMPT_FILTER_URL)));
    }

    @Test
    void shouldProcessValidUrlSuccessfully() {
        // Arrange
        when(message.text()).thenReturn(CMD_ADD_FILTER);
        addFilterCommand.execute(update, bot);
        when(message.text()).thenReturn(TEST_URL);

        // Act
        addFilterCommand.execute(update, bot);

        // Assert
        TrackingContext context = dialogService.getDialog(TEST_CHAT_ID);
        AssertionsForClassTypes.assertThat(context.url()).isEqualTo(TEST_URL);
        AssertionsForClassTypes.assertThat(context.trackState()).isEqualTo(TrackState.AWAITING_FILTER);
        Mockito.verify(bot)
                .execute(Mockito.argThat(
                        msg -> msg.getParameters().get(TELEGRAM_PARAM_CHAT_ID).equals(TEST_CHAT_ID)
                                && msg.getParameters().get(TELEGRAM_PARAM_TEXT).equals(ADD_FILTER_PROMPT_FILTER_NAME)));
    }

    @Test
    void shouldProcessInvalidFilterCount() {
        // Arrange
        when(message.text()).thenReturn(CMD_ADD_FILTER);
        addFilterCommand.execute(update, bot);
        when(message.text()).thenReturn(TEST_URL);
        addFilterCommand.execute(update, bot);
        when(message.text()).thenReturn(INVALID_FILTER_COUNT);

        // Act
        addFilterCommand.execute(update, bot);

        // Assert
        Mockito.verify(bot)
                .execute(Mockito.argThat(
                        msg -> msg.getParameters().get(TELEGRAM_PARAM_CHAT_ID).equals(TEST_CHAT_ID)
                                && msg.getParameters().get(TELEGRAM_PARAM_TEXT).equals(ADD_FILTER_ERROR_FILTER_COUNT)));
        TrackingContext context = dialogService.getDialog(TEST_CHAT_ID);
        AssertionsForClassTypes.assertThat(context).isNotNull();
        AssertionsForClassTypes.assertThat(context.trackState()).isEqualTo(TrackState.AWAITING_FILTER);
    }

    @Test
    void shouldProcessFilterInputAndCallAddFilterSuccessfully() {
        // Arrange
        when(message.text()).thenReturn(CMD_ADD_FILTER);
        addFilterCommand.execute(update, bot);
        when(message.text()).thenReturn(TEST_URL);
        addFilterCommand.execute(update, bot);
        when(message.text()).thenReturn(VALID_FILTER_COUNT);
        when(commandService.addFilterToTrackedLink(TEST_CHAT_ID, TEST_URL, VALID_FILTER_COUNT))
                .thenReturn(Mono.just(true));

        // Act
        addFilterCommand.execute(update, bot);

        // Assert
        Mockito.verify(bot)
                .execute(Mockito.argThat(msg -> msg.getParameters()
                                .get(TELEGRAM_PARAM_CHAT_ID)
                                .equals(TEST_CHAT_ID)
                        && msg.getParameters().get(TELEGRAM_PARAM_TEXT).equals(ADD_FILTER_SUCCESS_FILTER_ADDED)));
        AssertionsForClassTypes.assertThat(dialogService.getDialog(TEST_CHAT_ID))
                .isNull();
    }

    @Test
    void shouldProcessFilterInputAndCallAddFilterFailure() {
        // Arrange
        when(message.text()).thenReturn(CMD_ADD_FILTER);
        addFilterCommand.execute(update, bot);
        when(message.text()).thenReturn(TEST_URL);
        addFilterCommand.execute(update, bot);
        when(message.text()).thenReturn(VALID_FILTER_COUNT);
        when(commandService.addFilterToTrackedLink(TEST_CHAT_ID, TEST_URL, VALID_FILTER_COUNT))
                .thenReturn(Mono.just(false));

        // Act
        addFilterCommand.execute(update, bot);

        // Assert
        Mockito.verify(bot)
                .execute(Mockito.argThat(
                        msg -> msg.getParameters().get(TELEGRAM_PARAM_CHAT_ID).equals(TEST_CHAT_ID)
                                && msg.getParameters().get(TELEGRAM_PARAM_TEXT).equals(ADD_FILTER_ERROR_FILTER_ADD)));
        AssertionsForClassTypes.assertThat(dialogService.getDialog(TEST_CHAT_ID))
                .isNull();
    }
}
