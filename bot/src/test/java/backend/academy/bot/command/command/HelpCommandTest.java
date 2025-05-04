package backend.academy.bot.command.command;

import static backend.academy.bot.TestData.CMD_HELP;
import static backend.academy.bot.TestData.CMD_LIST;
import static backend.academy.bot.TestData.CMD_START;
import static backend.academy.bot.TestData.EXTRA_ARGUMENT;
import static backend.academy.bot.TestData.HELP_HEADER;
import static backend.academy.bot.TestData.HELP_LIST_DESCRIPTION;
import static backend.academy.bot.TestData.HELP_START_DESCRIPTION;
import static backend.academy.bot.TestData.TELEGRAM_PARAM_CHAT_ID;
import static backend.academy.bot.TestData.TELEGRAM_PARAM_TEXT;
import static backend.academy.bot.TestData.TEST_CHAT_ID;
import static backend.academy.bot.TestData.USAGE_HELP;
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
    private static final String SEPARATOR = " - ";

    private TelegramBot bot;
    private HelpCommand helpCommand;
    private Update update;
    private Message message;

    @BeforeEach
    void setUp() {
        bot = mock(TelegramBot.class);
        ApplicationContext context = mock(ApplicationContext.class);
        CommandHandler commandHandler = mock(CommandHandler.class);

        when(commandHandler.commandDescriptions())
                .thenReturn(Map.of(
                        CMD_START, HELP_START_DESCRIPTION,
                        CMD_LIST, HELP_LIST_DESCRIPTION));
        when(context.getBean(CommandHandler.class)).thenReturn(commandHandler);

        helpCommand = new HelpCommand(context);

        update = mock(Update.class);
        message = mock(Message.class);
        Chat chat = mock(Chat.class);

        when(update.message()).thenReturn(message);
        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(TEST_CHAT_ID);
    }

    @Test
    void shouldReturnHelpMessage() {
        when(message.text()).thenReturn(CMD_HELP);

        helpCommand.execute(update, bot);

        Mockito.verify(bot)
                .execute(Mockito.argThat(
                        msg -> msg.getParameters().get(TELEGRAM_PARAM_CHAT_ID).equals(TEST_CHAT_ID)
                                && msg.getParameters()
                                        .get(TELEGRAM_PARAM_TEXT)
                                        .toString()
                                        .contains(HELP_HEADER)
                                && msg.getParameters()
                                        .get(TELEGRAM_PARAM_TEXT)
                                        .toString()
                                        .contains(CMD_START + SEPARATOR + HELP_START_DESCRIPTION)
                                && msg.getParameters()
                                        .get(TELEGRAM_PARAM_TEXT)
                                        .toString()
                                        .contains(CMD_LIST + SEPARATOR + HELP_LIST_DESCRIPTION)));
    }

    @Test
    void shouldReturnUsageErrorForExtraArguments() {
        when(message.text()).thenReturn(CMD_HELP + EXTRA_ARGUMENT);

        helpCommand.execute(update, bot);

        Mockito.verify(bot)
                .execute(Mockito.argThat(
                        msg -> msg.getParameters().get(TELEGRAM_PARAM_CHAT_ID).equals(TEST_CHAT_ID)
                                && msg.getParameters().get(TELEGRAM_PARAM_TEXT).equals(USAGE_HELP)));
    }
}
