package backend.academy.bot.command.command;

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
import reactor.core.publisher.Mono;

class StartCommandTest {

    private TelegramBot bot;
    private CommandService commandService;
    private DialogService dialogService;
    private StartCommand startCommand;
    private Update update;
    private Message message;
    private Chat chat;

    @BeforeEach
    void setUp() {
        bot = mock(TelegramBot.class);
        commandService = mock(CommandService.class);
        dialogService = new DialogService();

        startCommand = new StartCommand(commandService, dialogService);

        update = mock(Update.class);
        message = mock(Message.class);
        chat = mock(Chat.class);

        when(update.message()).thenReturn(message);
        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(123L);
    }

    @Test
    void shouldInitiateDialogWithNotificationChoice() {
        when(message.text()).thenReturn("/start");

        startCommand.execute(update, bot);

        TrackingContext context = dialogService.getDialog(123L);
        AssertionsForClassTypes.assertThat(context).isNotNull();
        AssertionsForClassTypes.assertThat(context.dialogType()).isEqualTo(DialogType.START);
        AssertionsForClassTypes.assertThat(context.trackState()).isEqualTo(TrackState.AWAITING_NOTIFICATION_MODE);

        verify(bot)
                .execute(argThat((SendMessage m) ->
                        m.getParameters().get("text").toString().contains("Привет! Я бот для отслеживания ссылок")));
    }

    @Test
    void shouldRegisterImmediateModeAndCompleteDialog() {
        dialogService.startDialog(123L, DialogType.START);
        TrackingContext context = dialogService.getDialog(123L);
        context.trackState(TrackState.AWAITING_NOTIFICATION_MODE);

        when(message.text()).thenReturn("1");
        when(commandService.registerChat(123L, NotificationMode.IMMEDIATE, null))
                .thenReturn(Mono.just(true));

        startCommand.execute(update, bot);

        AssertionsForClassTypes.assertThat(dialogService.getDialog(123L)).isNull();
        verify(bot)
                .execute(
                        argThat((SendMessage m) -> m.getParameters().get("text").equals("Регистрация завершена!")));
    }

    @Test
    void shouldStartDigestModeFlow() {
        dialogService.startDialog(123L, DialogType.START);
        TrackingContext context = dialogService.getDialog(123L);
        context.trackState(TrackState.AWAITING_NOTIFICATION_MODE);

        when(message.text()).thenReturn("2");

        startCommand.execute(update, bot);

        context = dialogService.getDialog(123L);
        AssertionsForClassTypes.assertThat(context.notificationMode()).isEqualTo(NotificationMode.DAILY_DIGEST);
        AssertionsForClassTypes.assertThat(context.trackState()).isEqualTo(TrackState.AWAITING_NOTIFICATION_TIME);

        verify(bot)
                .execute(argThat((SendMessage m) ->
                        m.getParameters().get("text").toString().contains("Введите время")));
    }

    @Test
    void shouldHandleInvalidNotificationChoice() {
        dialogService.startDialog(123L, DialogType.START);
        TrackingContext context = dialogService.getDialog(123L);
        context.trackState(TrackState.AWAITING_NOTIFICATION_MODE);

        when(message.text()).thenReturn("3");

        startCommand.execute(update, bot);

        AssertionsForClassTypes.assertThat(context.trackState()).isEqualTo(TrackState.AWAITING_NOTIFICATION_MODE);
        verify(bot)
                .execute(argThat(
                        (SendMessage m) -> m.getParameters().get("text").equals("Пожалуйста, введите 1 или 2.")));
    }

    @Test
    void shouldHandleInvalidTimeFormat() {
        dialogService.startDialog(123L, DialogType.START);
        TrackingContext context = dialogService.getDialog(123L);
        context.trackState(TrackState.AWAITING_NOTIFICATION_TIME);
        context.notificationMode(NotificationMode.DAILY_DIGEST);

        when(message.text()).thenReturn("25:99");

        startCommand.execute(update, bot);

        AssertionsForClassTypes.assertThat(context.trackState()).isEqualTo(TrackState.AWAITING_NOTIFICATION_TIME);
        verify(bot)
                .execute(argThat((SendMessage m) ->
                        m.getParameters().get("text").toString().contains("Неверный формат времени")));
    }

    @Test
    void shouldRegisterDigestModeSuccessfullyAfterValidTime() {
        dialogService.startDialog(123L, DialogType.START);
        TrackingContext context = dialogService.getDialog(123L);
        context.trackState(TrackState.AWAITING_NOTIFICATION_TIME);
        context.notificationMode(NotificationMode.DAILY_DIGEST);

        when(message.text()).thenReturn("09:30");
        when(commandService.registerChat(123L, NotificationMode.DAILY_DIGEST, LocalTime.parse("09:30")))
                .thenReturn(Mono.just(true));

        startCommand.execute(update, bot);

        AssertionsForClassTypes.assertThat(dialogService.getDialog(123L)).isNull();
        verify(bot)
                .execute(
                        argThat((SendMessage m) -> m.getParameters().get("text").equals("Регистрация завершена!")));
    }
}
