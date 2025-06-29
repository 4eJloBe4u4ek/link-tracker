package backend.academy.bot.command.command;

import static backend.academy.bot.TestData.CMD_SET_MODE;
import static backend.academy.bot.TestData.ENTER_TIME_PROMPT;
import static backend.academy.bot.TestData.EXTRA_ARGUMENT;
import static backend.academy.bot.TestData.INVALID_CHOICE_MESSAGE;
import static backend.academy.bot.TestData.INVALID_TIME;
import static backend.academy.bot.TestData.INVALID_TIME_FORMAT_MESSAGE;
import static backend.academy.bot.TestData.SET_MODE_MODE_PROMPT;
import static backend.academy.bot.TestData.SET_MODE_UPDATE_SUCCESS;
import static backend.academy.bot.TestData.TELEGRAM_PARAM_CHAT_ID;
import static backend.academy.bot.TestData.TELEGRAM_PARAM_TEXT;
import static backend.academy.bot.TestData.TEST_CHAT_ID;
import static backend.academy.bot.TestData.USAGE_SET_MODE;
import static backend.academy.bot.TestData.VALID_TIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

class UpdateNotificationModeCommandTest {

    private TelegramBot bot;
    private CommandService commandService;
    private DialogService dialogService;
    private UpdateNotificationModeCommand command;
    private Update update;
    private Message message;

    @BeforeEach
    void setUp() {
        bot = mock(TelegramBot.class);
        commandService = mock(CommandService.class);
        dialogService = new DialogService();
        command = new UpdateNotificationModeCommand(commandService, dialogService);

        update = mock(Update.class);
        message = mock(Message.class);
        Chat chat = mock(Chat.class);

        when(update.message()).thenReturn(message);
        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(TEST_CHAT_ID);
    }

    @Test
    void shouldReturnUsageErrorForExtraArguments() {
        when(message.text()).thenReturn(CMD_SET_MODE + EXTRA_ARGUMENT);
        command.execute(update, bot);

        Mockito.verify(bot)
                .execute(argThat(
                        msg -> msg.getParameters().get(TELEGRAM_PARAM_CHAT_ID).equals(TEST_CHAT_ID)
                                && msg.getParameters().get(TELEGRAM_PARAM_TEXT).equals(USAGE_SET_MODE)));
    }

    @Test
    void shouldInitiateUpdateModeDialog() {
        when(message.text()).thenReturn(CMD_SET_MODE);

        command.execute(update, bot);

        TrackingContext context = dialogService.getDialog(TEST_CHAT_ID);
        assertThat(context).isNotNull();
        assertThat(context.dialogType()).isEqualTo(DialogType.UPDATE_NOTIFICATION_MODE);
        assertThat(context.trackState()).isEqualTo(TrackState.AWAITING_NOTIFICATION_MODE);

        verify(bot)
                .execute(argThat((SendMessage m) ->
                        m.getParameters().get(TELEGRAM_PARAM_TEXT).toString().contains(SET_MODE_MODE_PROMPT)));
    }

    @Test
    void shouldUpdateToImmediateMode() {
        dialogService.startDialog(TEST_CHAT_ID, DialogType.UPDATE_NOTIFICATION_MODE);
        TrackingContext context = dialogService.getDialog(TEST_CHAT_ID);
        context.trackState(TrackState.AWAITING_NOTIFICATION_MODE);

        when(message.text()).thenReturn("1");
        when(commandService.updateNotificationMode(TEST_CHAT_ID, NotificationMode.IMMEDIATE, null))
                .thenReturn(Mono.just(true));

        command.execute(update, bot);

        assertThat(dialogService.getDialog(TEST_CHAT_ID)).isNull();
        verify(bot)
                .execute(argThat((SendMessage m) ->
                        m.getParameters().get(TELEGRAM_PARAM_TEXT).equals(SET_MODE_UPDATE_SUCCESS)));
    }

    @Test
    void shouldStartDigestFlowAfterChoice() {
        dialogService.startDialog(TEST_CHAT_ID, DialogType.UPDATE_NOTIFICATION_MODE);
        TrackingContext context = dialogService.getDialog(TEST_CHAT_ID);
        context.trackState(TrackState.AWAITING_NOTIFICATION_MODE);

        when(message.text()).thenReturn("2");

        command.execute(update, bot);

        context = dialogService.getDialog(TEST_CHAT_ID);
        assertThat(context.notificationMode()).isEqualTo(NotificationMode.DAILY_DIGEST);
        assertThat(context.trackState()).isEqualTo(TrackState.AWAITING_NOTIFICATION_TIME);

        verify(bot)
                .execute(argThat((SendMessage m) ->
                        m.getParameters().get(TELEGRAM_PARAM_TEXT).toString().contains(ENTER_TIME_PROMPT)));
    }

    @Test
    void shouldRejectInvalidModeChoice() {
        dialogService.startDialog(TEST_CHAT_ID, DialogType.UPDATE_NOTIFICATION_MODE);
        TrackingContext context = dialogService.getDialog(TEST_CHAT_ID);
        context.trackState(TrackState.AWAITING_NOTIFICATION_MODE);

        when(message.text()).thenReturn("3");

        command.execute(update, bot);

        assertThat(context.trackState()).isEqualTo(TrackState.AWAITING_NOTIFICATION_MODE);
        verify(bot)
                .execute(argThat((SendMessage m) ->
                        m.getParameters().get(TELEGRAM_PARAM_TEXT).equals(INVALID_CHOICE_MESSAGE)));
    }

    @Test
    void shouldRejectInvalidTimeFormat() {
        dialogService.startDialog(TEST_CHAT_ID, DialogType.UPDATE_NOTIFICATION_MODE);
        TrackingContext context = dialogService.getDialog(TEST_CHAT_ID);
        context.trackState(TrackState.AWAITING_NOTIFICATION_TIME);
        context.notificationMode(NotificationMode.DAILY_DIGEST);

        when(message.text()).thenReturn(INVALID_TIME);

        command.execute(update, bot);

        assertThat(context.trackState()).isEqualTo(TrackState.AWAITING_NOTIFICATION_TIME);
        verify(bot)
                .execute(argThat((SendMessage m) ->
                        m.getParameters().get(TELEGRAM_PARAM_TEXT).toString().contains(INVALID_TIME_FORMAT_MESSAGE)));
    }

    @Test
    void shouldUpdateDigestModeAfterTimeEntry() {
        dialogService.startDialog(TEST_CHAT_ID, DialogType.UPDATE_NOTIFICATION_MODE);
        TrackingContext context = dialogService.getDialog(TEST_CHAT_ID);
        context.trackState(TrackState.AWAITING_NOTIFICATION_TIME);
        context.notificationMode(NotificationMode.DAILY_DIGEST);

        when(message.text()).thenReturn(VALID_TIME);
        when(commandService.updateNotificationMode(
                        TEST_CHAT_ID, NotificationMode.DAILY_DIGEST, LocalTime.parse(VALID_TIME)))
                .thenReturn(Mono.just(true));

        command.execute(update, bot);

        assertThat(dialogService.getDialog(TEST_CHAT_ID)).isNull();
        verify(bot)
                .execute(argThat((SendMessage m) ->
                        m.getParameters().get(TELEGRAM_PARAM_TEXT).equals(SET_MODE_UPDATE_SUCCESS)));
    }
}
