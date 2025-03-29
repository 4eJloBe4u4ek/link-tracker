package backend.academy.bot.command;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import backend.academy.bot.dialog.DialogService;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;

class CommandHandlerTest {
    @Test
    void shouldSendErrorForUnknownCommand() {
        TelegramBot bot = mock(TelegramBot.class);
        DialogService dialogService = mock(DialogService.class);
        ApplicationContext context = mock(ApplicationContext.class);
        CommandHandler commandHandler = new CommandHandler(dialogService, context);
        Update update = mock(Update.class);
        Message message = mock(Message.class);
        Chat chat = mock(Chat.class);
        when(update.message()).thenReturn(message);
        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(123L);
        when(message.text()).thenReturn("/unknown");

        commandHandler.handleCommand(update, bot);

        Mockito.verify(bot)
                .execute(
                        Mockito.argThat(
                                msg -> msg.getParameters().get("chat_id").equals(123L)
                                        && msg.getParameters()
                                                .get("text")
                                                .equals(
                                                        "Неизвестная команда. Используйте /help для просмотра списка доступных команд.")));
    }
}
