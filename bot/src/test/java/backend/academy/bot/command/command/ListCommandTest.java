package backend.academy.bot.command.command;

import static backend.academy.bot.TestData.CMD_LIST;
import static backend.academy.bot.TestData.EXTRA_ARGUMENT;
import static backend.academy.bot.TestData.LIST_EMPTY_MESSAGE;
import static backend.academy.bot.TestData.LIST_HEADER_MESSAGE;
import static backend.academy.bot.TestData.TELEGRAM_PARAM_CHAT_ID;
import static backend.academy.bot.TestData.TELEGRAM_PARAM_TEXT;
import static backend.academy.bot.TestData.TEST_CHAT_ID;
import static backend.academy.bot.TestData.TEST_URL;
import static backend.academy.bot.TestData.USAGE_LIST;
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

    @BeforeEach
    void setUp() {
        bot = mock(TelegramBot.class);
        commandService = mock(CommandService.class);

        listCommand = new ListCommand(commandService);

        update = mock(Update.class);
        message = mock(Message.class);
        Chat chat = mock(Chat.class);

        when(update.message()).thenReturn(message);
        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(TEST_CHAT_ID);
    }

    @Test
    void shouldReturnEmptyListMessageWhenNoLinks() {
        // Arrange
        when(message.text()).thenReturn(CMD_LIST);
        when(commandService.getTrackedLinks(TEST_CHAT_ID)).thenReturn(Mono.just(List.of()));

        // Act
        listCommand.execute(update, bot);

        // Assert
        Mockito.verify(bot)
                .execute(Mockito.argThat(
                        msg -> msg.getParameters().get(TELEGRAM_PARAM_CHAT_ID).equals(TEST_CHAT_ID)
                                && msg.getParameters().get(TELEGRAM_PARAM_TEXT).equals(LIST_EMPTY_MESSAGE)));
    }

    @Test
    void shouldReturnTrackedLinks() {
        // Arrange
        when(message.text()).thenReturn(CMD_LIST);
        when(commandService.getTrackedLinks(TEST_CHAT_ID)).thenReturn(Mono.just(List.of(TEST_URL)));

        // Act
        listCommand.execute(update, bot);

        // Assert
        Mockito.verify(bot)
                .execute(Mockito.argThat(
                        msg -> msg.getParameters().get(TELEGRAM_PARAM_CHAT_ID).equals(TEST_CHAT_ID)
                                && msg.getParameters()
                                        .get(TELEGRAM_PARAM_TEXT)
                                        .toString()
                                        .contains(LIST_HEADER_MESSAGE)
                                && msg.getParameters()
                                        .get(TELEGRAM_PARAM_TEXT)
                                        .toString()
                                        .contains(TEST_URL)));
    }

    @Test
    void shouldReturnUsageErrorForExtraArguments() {
        // Arrange
        when(message.text()).thenReturn(CMD_LIST + EXTRA_ARGUMENT);

        // Act
        listCommand.execute(update, bot);

        // Assert
        Mockito.verify(bot)
                .execute(Mockito.argThat(
                        msg -> msg.getParameters().get(TELEGRAM_PARAM_CHAT_ID).equals(TEST_CHAT_ID)
                                && msg.getParameters().get(TELEGRAM_PARAM_TEXT).equals(USAGE_LIST)));
    }
}
