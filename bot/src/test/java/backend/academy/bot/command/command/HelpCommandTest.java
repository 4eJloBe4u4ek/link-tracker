package backend.academy.bot.command.command;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import backend.academy.bot.command.CommandHandler;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;

class HelpCommandTest {
    private TelegramBot bot;
    private ApplicationContext context;
    private CommandHandler commandHandler;
    private HelpCommand helpCommand;
    private Update update;
    private Message message;
    private Chat chat;

    @BeforeEach
    void setUp() {
        bot = mock(TelegramBot.class);
        context = mock(ApplicationContext.class);
        commandHandler = mock(CommandHandler.class);

        when(commandHandler.commandDescriptions())
                .thenReturn(Map.of(
                        "/start", "Запуск бота.",
                        "/list", "Отслеживаемые ссылки."));
        when(context.getBean(CommandHandler.class)).thenReturn(commandHandler);

        helpCommand = new HelpCommand(context);

        update = mock(Update.class);
        message = mock(Message.class);
        chat = mock(Chat.class);

        when(update.message()).thenReturn(message);
        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(123L);
    }

    @Test
    void shouldReturnHelpMessage() {
        when(message.text()).thenReturn("/help");

        helpCommand.execute(update, bot);

        Mockito.verify(bot)
                .execute(Mockito.argThat(msg -> msg.getParameters()
                                .get("chat_id")
                                .equals(123L)
                        && msg.getParameters().get("text").toString().contains("Доступные команды:")
                        && msg.getParameters().get("text").toString().contains("/start - Запуск бота.")
                        && msg.getParameters().get("text").toString().contains("/list - Отслеживаемые ссылки.")));
    }

    @Test
    void shouldReturnUsageErrorForExtraArguments() {
        when(message.text()).thenReturn("/help something");

        helpCommand.execute(update, bot);

        Mockito.verify(bot)
                .execute(Mockito.argThat(
                        msg -> msg.getParameters().get("chat_id").equals(123L)
                                && msg.getParameters().get("text").equals("Использование: /help")));
    }
}
