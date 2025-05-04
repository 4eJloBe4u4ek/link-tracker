package backend.academy.bot.command.command;

import static backend.academy.bot.TestData.CMD_START;
import static backend.academy.bot.TestData.ENTER_TIME_PROMPT;
import static backend.academy.bot.TestData.EXTRA_ARGUMENT;
import static backend.academy.bot.TestData.INVALID_CHOICE_MESSAGE;
import static backend.academy.bot.TestData.INVALID_TIME;
import static backend.academy.bot.TestData.INVALID_TIME_FORMAT_MESSAGE;
import static backend.academy.bot.TestData.START_GREETING_MESSAGE;
import static backend.academy.bot.TestData.START_REGISTRATION_COMPLETE;
import static backend.academy.bot.TestData.TELEGRAM_PARAM_CHAT_ID;
import static backend.academy.bot.TestData.TELEGRAM_PARAM_TEXT;
import static backend.academy.bot.TestData.TEST_CHAT_ID;
import static backend.academy.bot.TestData.USAGE_START;
import static backend.academy.bot.TestData.VALID_TIME;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.bot.dialog.DialogService;
import backend.academy.bot.dialog.DialogType;
import backend.academy.bot.dialog.TrackState;
import backend.academy.bot.dialog.TrackingContext;
import backend.academy.bot.service.CommandService;
import backend.academy.shared.dto.NotificationMode;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.time.LocalTime;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

class StartCommandTest {

    private TelegramBot bot;
    private CommandService commandService;
    private DialogService dialogService;
    private StartCommand startCommand;
    private Update update;
    private Message message;

    @BeforeEach
    void setUp() {
        bot = mock(TelegramBot.class);
        commandService = mock(CommandService.class);
        dialogService = new DialogService();

        startCommand = new StartCommand(commandService, dialogService);

        update = mock(Update.class);
        message = mock(Message.class);
        Chat chat = mock(Chat.class);

        when(update.message()).thenReturn(message);
        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(TEST_CHAT_ID);
    }

    @Test
    void shouldReturnUsageErrorForExtraArguments() {
        when(message.text()).thenReturn(CMD_START + EXTRA_ARGUMENT);
        startCommand.execute(update, bot);

        Mockito.verify(bot)
                .execute(argThat(
                        msg -> msg.getParameters().get(TELEGRAM_PARAM_CHAT_ID).equals(TEST_CHAT_ID)
                                && msg.getParameters().get(TELEGRAM_PARAM_TEXT).equals(USAGE_START)));
    }

    @Test
    void shouldInitiateDialogWithNotificationChoice() {
        when(message.text()).thenReturn(CMD_START);

        startCommand.execute(update, bot);

        TrackingContext context = dialogService.getDialog(TEST_CHAT_ID);
        AssertionsForClassTypes.assertThat(context).isNotNull();
        AssertionsForClassTypes.assertThat(context.dialogType()).isEqualTo(DialogType.START);
        AssertionsForClassTypes.assertThat(context.trackState()).isEqualTo(TrackState.AWAITING_NOTIFICATION_MODE);

        verify(bot)
                .execute(argThat((SendMessage m) ->
                        m.getParameters().get(TELEGRAM_PARAM_TEXT).toString().contains(START_GREETING_MESSAGE)));
    }

    @Test
    void shouldRegisterImmediateModeAndCompleteDialog() {
        dialogService.startDialog(TEST_CHAT_ID, DialogType.START);
        TrackingContext context = dialogService.getDialog(TEST_CHAT_ID);
        context.trackState(TrackState.AWAITING_NOTIFICATION_MODE);

        when(message.text()).thenReturn("1");
        when(commandService.registerChat(TEST_CHAT_ID, NotificationMode.IMMEDIATE, null))
                .thenReturn(Mono.just(true));

        startCommand.execute(update, bot);

        AssertionsForClassTypes.assertThat(dialogService.getDialog(TEST_CHAT_ID))
                .isNull();
        verify(bot)
                .execute(argThat((SendMessage m) ->
                        m.getParameters().get(TELEGRAM_PARAM_TEXT).equals(START_REGISTRATION_COMPLETE)));
    }

    @Test
    void shouldStartDigestModeFlow() {
        dialogService.startDialog(TEST_CHAT_ID, DialogType.START);
        TrackingContext context = dialogService.getDialog(TEST_CHAT_ID);
        context.trackState(TrackState.AWAITING_NOTIFICATION_MODE);

        when(message.text()).thenReturn("2");

        startCommand.execute(update, bot);

        context = dialogService.getDialog(TEST_CHAT_ID);
        AssertionsForClassTypes.assertThat(context.notificationMode()).isEqualTo(NotificationMode.DAILY_DIGEST);
        AssertionsForClassTypes.assertThat(context.trackState()).isEqualTo(TrackState.AWAITING_NOTIFICATION_TIME);

        verify(bot)
                .execute(argThat((SendMessage m) ->
                        m.getParameters().get(TELEGRAM_PARAM_TEXT).toString().contains(ENTER_TIME_PROMPT)));
    }

    @Test
    void shouldHandleInvalidNotificationChoice() {
        dialogService.startDialog(TEST_CHAT_ID, DialogType.START);
        TrackingContext context = dialogService.getDialog(TEST_CHAT_ID);
        context.trackState(TrackState.AWAITING_NOTIFICATION_MODE);

        when(message.text()).thenReturn("3");

        startCommand.execute(update, bot);

        AssertionsForClassTypes.assertThat(context.trackState()).isEqualTo(TrackState.AWAITING_NOTIFICATION_MODE);
        verify(bot)
                .execute(argThat((SendMessage m) ->
                        m.getParameters().get(TELEGRAM_PARAM_TEXT).equals(INVALID_CHOICE_MESSAGE)));
    }

    @Test
    void shouldHandleInvalidTimeFormat() {
        dialogService.startDialog(TEST_CHAT_ID, DialogType.START);
        TrackingContext context = dialogService.getDialog(TEST_CHAT_ID);
        context.trackState(TrackState.AWAITING_NOTIFICATION_TIME);
        context.notificationMode(NotificationMode.DAILY_DIGEST);

        when(message.text()).thenReturn(INVALID_TIME);

        startCommand.execute(update, bot);

        AssertionsForClassTypes.assertThat(context.trackState()).isEqualTo(TrackState.AWAITING_NOTIFICATION_TIME);
        verify(bot)
                .execute(argThat((SendMessage m) ->
                        m.getParameters().get(TELEGRAM_PARAM_TEXT).toString().contains(INVALID_TIME_FORMAT_MESSAGE)));
    }

    @Test
    void shouldRegisterDigestModeSuccessfullyAfterValidTime() {
        dialogService.startDialog(TEST_CHAT_ID, DialogType.START);
        TrackingContext context = dialogService.getDialog(TEST_CHAT_ID);
        context.trackState(TrackState.AWAITING_NOTIFICATION_TIME);
        context.notificationMode(NotificationMode.DAILY_DIGEST);

        when(message.text()).thenReturn(VALID_TIME);
        when(commandService.registerChat(TEST_CHAT_ID, NotificationMode.DAILY_DIGEST, LocalTime.parse(VALID_TIME)))
                .thenReturn(Mono.just(true));

        startCommand.execute(update, bot);

        AssertionsForClassTypes.assertThat(dialogService.getDialog(TEST_CHAT_ID))
                .isNull();
        verify(bot)
                .execute(argThat((SendMessage m) ->
                        m.getParameters().get(TELEGRAM_PARAM_TEXT).equals(START_REGISTRATION_COMPLETE)));
    }
}
