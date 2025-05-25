package backend.academy.bot.command;

import static backend.academy.bot.TestData.TELEGRAM_PARAM_CHAT_ID;
import static backend.academy.bot.TestData.TELEGRAM_PARAM_TEXT;
import static backend.academy.bot.TestData.TEST_CHAT_ID;
import static backend.academy.bot.TestData.UNKNOWN_COMMAND_ERROR_MESSAGE;
import static backend.academy.bot.TestData.UNKNOWN_COMMAND_INPUT;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import backend.academy.bot.dialog.DialogService;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;

class CommandHandlerTest {
    TelegramBot bot = mock(TelegramBot.class);
    DialogService dialogService = mock(DialogService.class);
    ApplicationContext context = mock(ApplicationContext.class);
    Update update = mock(Update.class);
    Message message = mock(Message.class);
    Chat chat = mock(Chat.class);
    SimpleMeterRegistry registry;
    CommandHandler commandHandler;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        Metrics.globalRegistry.add(registry);
        commandHandler = new CommandHandler(dialogService, context, registry);
    }

    @AfterEach
    void tearDown() {
        registry.clear();
        Metrics.globalRegistry.clear();
    }

    @Test
    void shouldSendErrorForUnknownCommand() {
        when(update.message()).thenReturn(message);
        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(TEST_CHAT_ID);
        when(message.text()).thenReturn(UNKNOWN_COMMAND_INPUT);

        commandHandler.handleCommand(update, bot);

        Mockito.verify(bot)
                .execute(Mockito.argThat(
                        msg -> msg.getParameters().get(TELEGRAM_PARAM_CHAT_ID).equals(TEST_CHAT_ID)
                                && msg.getParameters().get(TELEGRAM_PARAM_TEXT).equals(UNKNOWN_COMMAND_ERROR_MESSAGE)));
    }

    @Test
    void shouldIncrementCustomUserMessagesCounter() {
        when(update.message()).thenReturn(message);
        when(update.message()).thenReturn(message);
        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(TEST_CHAT_ID);
        when(message.text()).thenReturn(UNKNOWN_COMMAND_INPUT);

        commandHandler.handleCommand(update, bot);

        Counter counter = registry.find("custom_user_messages_total").counter();
        Assertions.assertNotNull(counter);
        Assertions.assertEquals(1, counter.count());
    }
}
