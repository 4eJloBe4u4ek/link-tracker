package backend.academy.bot.command.command;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import backend.academy.bot.service.CommandService;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

class StartCommandTest {
    private TelegramBot bot;
    private CommandService commandService;
    private StartCommand startCommand;
    private Update update;
    private Message message;
    private Chat chat;

    @BeforeEach
    void setUp() {
        bot = mock(TelegramBot.class);
        commandService = mock(CommandService.class);

        startCommand = new StartCommand(commandService);

        update = mock(Update.class);
        message = mock(Message.class);
        chat = mock(Chat.class);

        when(update.message()).thenReturn(message);
        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(123L);
    }

    @Test
    void shouldReturnSuccessMessageWhenChatRegistered() {
        when(message.text()).thenReturn("/start");
        when(commandService.registerChat(123L)).thenReturn(Mono.just(true));

        startCommand.execute(update, bot);

        Mockito.verify(bot)
                .execute(Mockito.argThat(
                        msg -> msg.getParameters().get("chat_id").equals(123L)
                                && msg.getParameters().get("text").equals("Привет! Я бот для отслеживания ссылок.")));
    }

    @Test
    void shouldReturnAlreadyExistsMessageWhenChatExists() {
        when(message.text()).thenReturn("/start");
        when(commandService.registerChat(123L)).thenReturn(Mono.just(false));

        startCommand.execute(update, bot);

        Mockito.verify(bot)
                .execute(Mockito.argThat(
                        msg -> msg.getParameters().get("chat_id").equals(123L)
                                && msg.getParameters().get("text").equals("Чат уже существует!")));
    }

    @Test
    void shouldReturnUsageErrorForExtraArguments() {
        when(message.text()).thenReturn("/start extra");

        startCommand.execute(update, bot);

        Mockito.verify(bot)
                .execute(Mockito.argThat(
                        msg -> msg.getParameters().get("chat_id").equals(123L)
                                && msg.getParameters().get("text").equals("Использование: /start")));
    }
}
