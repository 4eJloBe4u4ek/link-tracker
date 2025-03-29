package backend.academy.bot.command.command;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import backend.academy.bot.dialog.DialogService;
import backend.academy.bot.dialog.DialogType;
import backend.academy.bot.dialog.TrackState;
import backend.academy.bot.dialog.TrackingContext;
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

class TrackCommandTest {
    private TelegramBot bot;
    private CommandService commandService;
    private DialogService dialogService;
    private TrackCommand trackCommand;
    private Update update;
    private Message message;
    private Chat chat;

    @BeforeEach
    void setUp() {
        bot = mock(TelegramBot.class);
        commandService = mock(CommandService.class);
        dialogService = new DialogService();
        trackCommand = new TrackCommand(commandService, dialogService);

        update = mock(Update.class);
        message = mock(Message.class);
        chat = mock(Chat.class);

        when(update.message()).thenReturn(message);
        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(123L);
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
    void shouldStartTrackDialog() {
        when(message.text()).thenReturn("/track");

        trackCommand.execute(update, bot);

        assertThat(dialogService.getDialog(123L).dialogType()).isEqualTo(DialogType.TRACK);
        Mockito.verify(bot)
                .execute(Mockito.argThat(
                        msg -> msg.getParameters().get("chat_id").equals(123L)
                                && msg.getParameters().get("text").equals("Укажите ссылку для отслеживания.")));
    }

    @Test
    void shouldProcessValidUrlSuccessfully() {
        when(message.text()).thenReturn("/track");
        trackCommand.execute(update, bot);
        when(message.text()).thenReturn("http://example.com");
        trackCommand.execute(update, bot);

        TrackingContext context = dialogService.getDialog(123L);
        assertThat(context.url()).isEqualTo("http://example.com");
        assertThat(context.trackState()).isEqualTo(TrackState.AWAITING_TAGS);

        Mockito.verify(bot)
                .execute(Mockito.argThat(msg -> msg.getParameters()
                                .get("chat_id")
                                .equals(123L)
                        && msg.getParameters().get("text").equals("Введите теги через пробел (опционально - /skip):")));
    }

    @Test
    void shouldProcessTagsInputSuccessfully() {
        when(message.text()).thenReturn("/track");
        trackCommand.execute(update, bot);
        when(message.text()).thenReturn("http://example.com");
        trackCommand.execute(update, bot);
        when(message.text()).thenReturn("tag1 tag2");
        trackCommand.execute(update, bot);

        TrackingContext context = dialogService.getDialog(123L);
        List<String> tags = context.tags();
        assertThat(tags).isEqualTo(List.of("tag1", "tag2"));
        assertThat(context.trackState()).isEqualTo(TrackState.AWAITING_FILTERS);

        Mockito.verify(bot)
                .execute(Mockito.argThat(
                        msg -> msg.getParameters().get("chat_id").equals(123L)
                                && msg.getParameters()
                                        .get("text")
                                        .equals("Введите фильтры через пробел (опционально - /skip):")));
    }

    @Test
    void shouldProcessFiltersInputAndCallTrackLinkSuccessfully() {
        when(message.text()).thenReturn("/track");
        trackCommand.execute(update, bot);
        when(message.text()).thenReturn("http://example.com");
        trackCommand.execute(update, bot);
        when(message.text()).thenReturn("tag1 tag2");
        trackCommand.execute(update, bot);
        when(message.text()).thenReturn("filter1 filter2");
        when(commandService.trackLink(
                        123L, "http://example.com", List.of("tag1", "tag2"), List.of("filter1", "filter2")))
                .thenReturn(Mono.just(true));

        trackCommand.execute(update, bot);

        Mockito.verify(bot)
                .execute(Mockito.argThat(
                        msg -> msg.getParameters().get("chat_id").equals(123L)
                                && msg.getParameters()
                                        .get("text")
                                        .equals("Ссылка http://example.com добавлена в отслеживание.")));
        assertThat(dialogService.getDialog(123L)).isNull();
    }

    @Test
    void shouldProcessFiltersInputAndCallTrackLinkFailure() {
        when(message.text()).thenReturn("/track");
        trackCommand.execute(update, bot);
        when(message.text()).thenReturn("http://example.com");
        trackCommand.execute(update, bot);
        when(message.text()).thenReturn("tag1 tag2");
        trackCommand.execute(update, bot);
        when(message.text()).thenReturn("filter1 filter2");
        when(commandService.trackLink(
                        123L, "http://example.com", List.of("tag1", "tag2"), List.of("filter1", "filter2")))
                .thenReturn(Mono.just(false));

        trackCommand.execute(update, bot);

        Mockito.verify(bot)
                .execute(Mockito.argThat(
                        msg -> msg.getParameters().get("chat_id").equals(123L)
                                && msg.getParameters()
                                        .get("text")
                                        .equals("Ошибка! Ссылка http://example.com не добавлена в отслеживание.")));
        assertThat(dialogService.getDialog(123L)).isNull();
    }
}
