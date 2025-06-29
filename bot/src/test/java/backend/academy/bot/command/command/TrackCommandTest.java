package backend.academy.bot.command.command;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import backend.academy.bot.dialog.DialogService;
import backend.academy.bot.dialog.DialogType;
import backend.academy.bot.dialog.TrackingContext;
import backend.academy.bot.service.CommandService;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class TrackCommandTest {
    private TelegramBot bot;
    private CommandService commandService;
    private DialogService dialogService;
    private TrackCommand trackCommand;
    private Update update;
    private Message message;
    private Chat chat;
    private TrackingContext trackingContext;

    @BeforeEach
    void setUp() {
        bot = mock(TelegramBot.class);
        commandService = mock(CommandService.class);
        dialogService = mock(DialogService.class);
        trackCommand = new TrackCommand(commandService, dialogService);

        update = mock(Update.class);
        message = mock(Message.class);
        chat = mock(Chat.class);

        when(update.message()).thenReturn(message);
        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(123L);

        trackingContext = new TrackingContext(123L, DialogType.TRACK);
    }

    @Test
    void shouldReturnUsageErrorForExtraArguments() {
        when(message.text()).thenReturn("/track extra");

        trackCommand.execute(update, bot);

        Mockito.verify(bot)
                .execute(Mockito.argThat(
                        msg -> msg.getParameters().get("chat_id").equals(123L)
                                && msg.getParameters().get("text").equals("Использование: /track")));
    }

    @Test
    void shouldStartTrackingDialog() {
        when(message.text()).thenReturn("/track");
        when(dialogService.getDialog(123L)).thenReturn(null);

        trackCommand.execute(update, bot);

        Mockito.verify(dialogService).startDialog(123L, DialogType.TRACK);
        Mockito.verify(bot)
                .execute(Mockito.argThat(
                        msg -> msg.getParameters().get("chat_id").equals(123L)
                                && msg.getParameters().get("text").equals("Укажите ссылку для отслеживания.")));
    }
}
