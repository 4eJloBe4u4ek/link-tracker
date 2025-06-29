package backend.academy.bot.command.command;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import backend.academy.bot.service.CommandService;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

class ListCommandTest {
    private TelegramBot bot;
    private CommandService commandService;
    private ListCommand listCommand;
    private Update update;
    private Message message;
    private Chat chat;

    @BeforeEach
    void setUp() {
        bot = mock(TelegramBot.class);
        commandService = mock(CommandService.class);

        listCommand = new ListCommand(commandService);

        update = mock(Update.class);
        message = mock(Message.class);
        chat = mock(Chat.class);

        when(update.message()).thenReturn(message);
        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(123L);
    }

    @Test
    void shouldReturnEmptyListMessageWhenNoLinks() {
        when(message.text()).thenReturn("/list");
        when(commandService.getTrackedLinks(123L)).thenReturn(Mono.just(List.of()));

        listCommand.execute(update, bot);

        Mockito.verify(bot)
                .execute(Mockito.argThat(
                        msg -> msg.getParameters().get("chat_id").equals(123L)
                                && msg.getParameters().get("text").equals("Список отслеживаемых ссылок пуст.")));
    }

    @Test
    void shouldReturnTrackedLinks() {
        when(message.text()).thenReturn("/list");
        when(commandService.getTrackedLinks(123L))
                .thenReturn(Mono.just(List.of(
                        "https://github.com/pengrad/java-telegram-bot-api", "https://stackoverflow.com/questions")));

        listCommand.execute(update, bot);

        Mockito.verify(bot)
                .execute(Mockito.argThat(msg -> msg.getParameters()
                                .get("chat_id")
                                .equals(123L)
                        && msg.getParameters().get("text").toString().contains("Список отслеживаемых ссылок:")
                        && msg.getParameters()
                                .get("text")
                                .toString()
                                .contains("https://github.com/pengrad/java-telegram-bot-api")
                        && msg.getParameters().get("text").toString().contains("https://stackoverflow.com/questions")));
    }

    @Test
    void shouldReturnUsageErrorForExtraArguments() {
        when(message.text()).thenReturn("/list extra");

        listCommand.execute(update, bot);

        Mockito.verify(bot)
                .execute(Mockito.argThat(
                        msg -> msg.getParameters().get("chat_id").equals(123L)
                                && msg.getParameters().get("text").equals("Использование: /list")));
    }
}
