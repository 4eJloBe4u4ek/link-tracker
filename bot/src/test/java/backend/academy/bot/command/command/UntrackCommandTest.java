package backend.academy.bot.command.command;

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
    private Chat chat;

    @BeforeEach
    void setUp() {
        bot = mock(TelegramBot.class);
        commandService = mock(CommandService.class);
        dialogService = new DialogService();
        untrackCommand = new UntrackCommand(commandService, dialogService);

        update = mock(Update.class);
        message = mock(Message.class);
        chat = mock(Chat.class);

        when(update.message()).thenReturn(message);
        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(123L);
    }

    @Test
    void shouldReturnUsageErrorForExtraArguments() {
        when(message.text()).thenReturn("/untrack extra");

        untrackCommand.execute(update, bot);

        Mockito.verify(bot)
                .execute(Mockito.argThat(
                        msg -> msg.getParameters().get("chat_id").equals(123L)
                                && msg.getParameters().get("text").equals("Использование: /untrack")));
    }

    @Test
    void shouldStartUntrackDialog() {
        when(message.text()).thenReturn("/untrack");

        untrackCommand.execute(update, bot);

        assertThat(dialogService.getDialog(123L).dialogType()).isEqualTo(DialogType.UNTRACK);
        Mockito.verify(bot)
                .execute(Mockito.argThat(msg -> msg.getParameters()
                                .get("chat_id")
                                .equals(123L)
                        && msg.getParameters().get("text").equals("Укажите ссылку для прекращения отслеживания.")));
    }

    @Test
    void shouldUntrackValidUrlSuccessfully() {
        dialogService.startDialog(123L, DialogType.UNTRACK);
        dialogService.getDialog(123L).trackState(TrackState.AWAITING_URL);
        when(message.text()).thenReturn("http://example.com");
        when(commandService.untrackLink(123L, "http://example.com")).thenReturn(Mono.just(true));

        untrackCommand.execute(update, bot);

        Mockito.verify(bot)
                .execute(Mockito.argThat(
                        msg -> msg.getParameters().get("chat_id").equals(123L)
                                && msg.getParameters()
                                        .get("text")
                                        .equals("Ссылка http://example.com больше не отслеживается")));
        assertThat(dialogService.getDialog(123L)).isNull();
    }

    @Test
    void shouldHandleUntrackFailure() {
        dialogService.startDialog(123L, DialogType.UNTRACK);
        dialogService.getDialog(123L).trackState(TrackState.AWAITING_URL);
        when(message.text()).thenReturn("http://example.com");
        when(commandService.untrackLink(123L, "http://example.com")).thenReturn(Mono.just(false));

        untrackCommand.execute(update, bot);

        Mockito.verify(bot)
                .execute(
                        Mockito.argThat(
                                msg -> msg.getParameters().get("chat_id").equals(123L)
                                        && msg.getParameters()
                                                .get("text")
                                                .equals(
                                                        "Ошибка! Невозможно прекратить отслеживание ссылки: http://example.com")));
        assertThat(dialogService.getDialog(123L)).isNull();
    }
}
