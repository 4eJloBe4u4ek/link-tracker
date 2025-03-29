package backend.academy.bot.command.command;

import static org.assertj.core.api.Assertions.assertThat;
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

class LinksByTagCommandTest {
    private TelegramBot bot;
    private CommandService commandService;
    private DialogService dialogService;
    private LinksByTagCommand linksByTagCommand;
    private Update update;
    private Message message;
    private Chat chat;

    @BeforeEach
    void setUp() {
        bot = mock(TelegramBot.class);
        commandService = mock(CommandService.class);
        dialogService = new DialogService();
        linksByTagCommand = new LinksByTagCommand(commandService, dialogService);

        update = mock(Update.class);
        message = mock(Message.class);
        chat = mock(Chat.class);

        when(update.message()).thenReturn(message);
        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(123L);
    }

    @Test
    void shouldReturnUsageErrorForExtraArguments() {
        when(message.text()).thenReturn("/linksbytag extra");
        linksByTagCommand.execute(update, bot);

        Mockito.verify(bot)
                .execute(Mockito.argThat(
                        msg -> msg.getParameters().get("chat_id").equals(123L)
                                && msg.getParameters().get("text").equals("Использование: /linksbytag")));
    }

    @Test
    void shouldStartLinksByTagDialog() {
        when(message.text()).thenReturn("/linksbytag");
        assertThat(dialogService.getDialog(123L)).isNull();

        linksByTagCommand.execute(update, bot);

        TrackingContext context = dialogService.getDialog(123L);
        assertThat(context).isNotNull();
        assertThat(context.dialogType()).isEqualTo(DialogType.LINKS_BY_TAG);
        assertThat(context.trackState()).isEqualTo(TrackState.AWAITING_TAG);
        Mockito.verify(bot)
                .execute(Mockito.argThat(msg -> msg.getParameters()
                                .get("chat_id")
                                .equals(123L)
                        && msg.getParameters().get("text").equals("Укажите тег для вывода отслеживаемых ссылок.")));
    }

    @Test
    void shouldProcessInvalidTagCount() {
        when(message.text()).thenReturn("/linksbytag");
        linksByTagCommand.execute(update, bot);
        when(message.text()).thenReturn("tag1 tag2");
        linksByTagCommand.execute(update, bot);

        Mockito.verify(bot)
                .execute(Mockito.argThat(
                        msg -> msg.getParameters().get("chat_id").equals(123L)
                                && msg.getParameters()
                                        .get("text")
                                        .equals("Введите один тег для вывода списка отслеживаемых ссылок.")));
        TrackingContext context = dialogService.getDialog(123L);
        assertThat(context).isNotNull();
        assertThat(context.trackState()).isEqualTo(TrackState.AWAITING_TAG);
    }

    @Test
    void shouldProcessValidTagInputEmptyList() {
        when(message.text()).thenReturn("/linksbytag");
        linksByTagCommand.execute(update, bot);
        when(message.text()).thenReturn("tag1");
        when(commandService.getTrackedLinksByTag(123L, "tag1")).thenReturn(Mono.just(List.of()));

        linksByTagCommand.execute(update, bot);

        Mockito.verify(bot)
                .execute(Mockito.argThat(msg -> msg.getParameters()
                                .get("chat_id")
                                .equals(123L)
                        && msg.getParameters().get("text").equals("Список отслеживаемых ссылок по тегу пуст.")));
        assertThat(dialogService.getDialog(123L)).isNull();
    }

    @Test
    void shouldHandleErrorWhenGettingLinks() {
        when(message.text()).thenReturn("/linksbytag");
        linksByTagCommand.execute(update, bot);
        when(message.text()).thenReturn("tag1");
        when(commandService.getTrackedLinksByTag(123L, "tag1")).thenReturn(Mono.error(new RuntimeException("Error")));

        linksByTagCommand.execute(update, bot);

        Mockito.verify(bot)
                .execute(Mockito.argThat(
                        msg -> msg.getParameters().get("chat_id").equals(123L)
                                && msg.getParameters()
                                        .get("text")
                                        .equals("Произошла ошибка при получении отслеживаемых ссылок по тегу.")));
        assertThat(dialogService.getDialog(123L)).isNull();
    }
}
