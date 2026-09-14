package backend.academy.bot.command.command;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SourcesCommandTest {
    private TelegramBot bot;
    private SourcesCommand sourcesCommand;
    private Update update;
    private Message message;

    @BeforeEach
    void setUp() {
        bot = mock(TelegramBot.class);
        sourcesCommand = new SourcesCommand();
        update = mock(Update.class);
        message = mock(Message.class);
        Chat chat = mock(Chat.class);

        when(update.message()).thenReturn(message);
        when(message.chat()).thenReturn(chat);
    }

    @Test
    void shouldReturnSupportedSources() {
        // Arrange
        when(message.text()).thenReturn("/sources");

        // Act
        sourcesCommand.execute(update, bot);

        // Assert
        verify(bot).execute(argThat(request -> {
            String text = request.getParameters().get("text").toString();
            return text.contains("https://github.com/spring-projects/spring-boot")
                    && text.contains("https://stackoverflow.com/questions/11227809/")
                    && text.contains("https://www.ticketpro.by/koncertnye-ploshhadki/dvorec-respubliki/")
                    && text.contains("сам каталог отслеживать нельзя")
                    && text.contains("/track");
        }));
    }

    @Test
    void shouldReturnUsageErrorForExtraArguments() {
        // Arrange
        when(message.text()).thenReturn("/sources extra");

        // Act
        sourcesCommand.execute(update, bot);

        // Assert
        verify(bot).execute(argThat(request ->
                request.getParameters().get("text").equals("Использование: /sources")));
    }
}
