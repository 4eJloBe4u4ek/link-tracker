package backend.academy.bot.command.command;

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
import reactor.core.publisher.Mono;

class UpdateNotificationModeCommandTest {

    private TelegramBot bot;
    private CommandService commandService;
    private DialogService dialogService;
    private UpdateNotificationModeCommand command;
    private Update update;
    private Message message;
    private Chat chat;

    @BeforeEach
    void setUp() {
        bot = mock(TelegramBot.class);
        commandService = mock(CommandService.class);
        dialogService = new DialogService();
        command = new UpdateNotificationModeCommand(commandService, dialogService);

        update = mock(Update.class);
        message = mock(Message.class);
        chat = mock(Chat.class);

        when(update.message()).thenReturn(message);
        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(123L);
    }

    @Test
    void shouldInitiateUpdateModeDialog() {
        when(message.text()).thenReturn("/setmode");

        command.execute(update, bot);

        TrackingContext context = dialogService.getDialog(123L);
        assertThat(context).isNotNull();
        assertThat(context.dialogType()).isEqualTo(DialogType.UPDATE_NOTIFICATION_MODE);
        assertThat(context.trackState()).isEqualTo(TrackState.AWAITING_NOTIFICATION_MODE);

        verify(bot)
                .execute(argThat((SendMessage m) ->
                        m.getParameters().get("text").toString().contains("Выберите новый режим уведомлений")));
    }

    @Test
    void shouldUpdateToImmediateMode() {
        dialogService.startDialog(123L, DialogType.UPDATE_NOTIFICATION_MODE);
        TrackingContext context = dialogService.getDialog(123L);
        context.trackState(TrackState.AWAITING_NOTIFICATION_MODE);

        when(message.text()).thenReturn("1");
        when(commandService.updateNotificationMode(123L, NotificationMode.IMMEDIATE, null))
                .thenReturn(Mono.just(true));

        command.execute(update, bot);

        assertThat(dialogService.getDialog(123L)).isNull();
        verify(bot)
                .execute(
                        argThat((SendMessage m) -> m.getParameters().get("text").equals("Режим уведомлений обновлен")));
    }

    @Test
    void shouldStartDigestFlowAfterChoice() {
        dialogService.startDialog(123L, DialogType.UPDATE_NOTIFICATION_MODE);
        TrackingContext context = dialogService.getDialog(123L);
        context.trackState(TrackState.AWAITING_NOTIFICATION_MODE);

        when(message.text()).thenReturn("2");

        command.execute(update, bot);

        context = dialogService.getDialog(123L);
        assertThat(context.notificationMode()).isEqualTo(NotificationMode.DAILY_DIGEST);
        assertThat(context.trackState()).isEqualTo(TrackState.AWAITING_NOTIFICATION_TIME);

        verify(bot)
                .execute(argThat((SendMessage m) ->
                        m.getParameters().get("text").toString().contains("Введите время")));
    }

    @Test
    void shouldRejectInvalidModeChoice() {
        dialogService.startDialog(123L, DialogType.UPDATE_NOTIFICATION_MODE);
        TrackingContext context = dialogService.getDialog(123L);
        context.trackState(TrackState.AWAITING_NOTIFICATION_MODE);

        when(message.text()).thenReturn("abc");

        command.execute(update, bot);

        assertThat(context.trackState()).isEqualTo(TrackState.AWAITING_NOTIFICATION_MODE);
        verify(bot)
                .execute(argThat(
                        (SendMessage m) -> m.getParameters().get("text").equals("Пожалуйста, введите 1 или 2.")));
    }

    @Test
    void shouldRejectInvalidTimeFormat() {
        dialogService.startDialog(123L, DialogType.UPDATE_NOTIFICATION_MODE);
        TrackingContext context = dialogService.getDialog(123L);
        context.trackState(TrackState.AWAITING_NOTIFICATION_TIME);
        context.notificationMode(NotificationMode.DAILY_DIGEST);

        when(message.text()).thenReturn("invalid");

        command.execute(update, bot);

        assertThat(context.trackState()).isEqualTo(TrackState.AWAITING_NOTIFICATION_TIME);
        verify(bot)
                .execute(argThat((SendMessage m) ->
                        m.getParameters().get("text").toString().contains("Неверный формат времени")));
    }

    @Test
    void shouldUpdateDigestModeAfterTimeEntry() {
        dialogService.startDialog(123L, DialogType.UPDATE_NOTIFICATION_MODE);
        TrackingContext context = dialogService.getDialog(123L);
        context.trackState(TrackState.AWAITING_NOTIFICATION_TIME);
        context.notificationMode(NotificationMode.DAILY_DIGEST);

        when(message.text()).thenReturn("08:15");
        when(commandService.updateNotificationMode(123L, NotificationMode.DAILY_DIGEST, LocalTime.parse("08:15")))
                .thenReturn(Mono.just(true));

        command.execute(update, bot);

        assertThat(dialogService.getDialog(123L)).isNull();
        verify(bot)
                .execute(
                        argThat((SendMessage m) -> m.getParameters().get("text").equals("Режим уведомлений обновлен")));
    }
}
