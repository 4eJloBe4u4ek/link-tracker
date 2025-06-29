package backend.academy.bot.command.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

class RemoveTagCommandTest {
    private TelegramBot bot;
    private CommandService commandService;
    private DialogService dialogService;
    private RemoveTagCommand removeTagCommand;
    private Update update;
    private Message message;
    private Chat chat;

    @BeforeEach
    void setUp() {
        bot = mock(TelegramBot.class);
        commandService = mock(CommandService.class);
        dialogService = new DialogService();
        removeTagCommand = new RemoveTagCommand(commandService, dialogService);

        update = mock(Update.class);
        message = mock(Message.class);
        chat = mock(Chat.class);

        when(update.message()).thenReturn(message);
        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(123L);
    }

    @Test
    void shouldReturnUsageErrorForExtraArguments() {
        when(message.text()).thenReturn("/removetag extra");
        removeTagCommand.execute(update, bot);

        Mockito.verify(bot)
                .execute(argThat(msg -> msg.getParameters().get("chat_id").equals(123L)
                        && msg.getParameters().get("text").equals("Использование: /removetag")));
    }

    @Test
    void shouldStartRemoveTagDialog() {
        when(message.text()).thenReturn("/removetag");
        assertThat(dialogService.getDialog(123L)).isNull();

        removeTagCommand.execute(update, bot);

        TrackingContext context = dialogService.getDialog(123L);
        assertThat(context).isNotNull();
        assertThat(context.dialogType()).isEqualTo(DialogType.REMOVE_TAG);
        assertThat(context.trackState()).isEqualTo(TrackState.AWAITING_URL);

        Mockito.verify(bot)
                .execute(argThat(msg -> msg.getParameters().get("chat_id").equals(123L)
                        && msg.getParameters().get("text").equals("Укажите ссылку для удаления тега.")));
    }

    @Test
    void shouldProcessValidUrlAndPromptTag() {
        when(message.text()).thenReturn("/removetag");
        removeTagCommand.execute(update, bot);
        when(message.text()).thenReturn("http://example.com");
        removeTagCommand.execute(update, bot);

        TrackingContext ctx = dialogService.getDialog(123L);
        assertThat(ctx.url()).isEqualTo("http://example.com");
        assertThat(ctx.trackState()).isEqualTo(TrackState.AWAITING_TAG);

        Mockito.verify(bot)
                .execute(argThat(msg -> msg.getParameters().get("chat_id").equals(123L)
                        && msg.getParameters().get("text").equals("Укажите тег для удаления.")));
    }

    @Test
    void shouldProcessInvalidTagCount() {
        when(message.text()).thenReturn("/removetag");
        removeTagCommand.execute(update, bot);
        when(message.text()).thenReturn("http://example.com");
        removeTagCommand.execute(update, bot);
        when(message.text()).thenReturn("tag1 tag2");
        removeTagCommand.execute(update, bot);

        Mockito.verify(bot)
                .execute(argThat(msg -> msg.getParameters().get("chat_id").equals(123L)
                        && msg.getParameters().get("text").equals("Введите один тег для удаления у ссылки.")));

        TrackingContext context = dialogService.getDialog(123L);
        assertThat(context).isNotNull();
        assertThat(context.trackState()).isEqualTo(TrackState.AWAITING_TAG);
    }

    @Test
    void shouldProcessTagInputAndCallRemoveTagSuccessfully() {
        when(message.text()).thenReturn("/removetag");
        removeTagCommand.execute(update, bot);
        when(message.text()).thenReturn("http://example.com");
        removeTagCommand.execute(update, bot);
        when(message.text()).thenReturn("tag1");
        when(commandService.removeTagFromTrackedLink(123L, "http://example.com", "tag1"))
                .thenReturn(Mono.just(true));
        removeTagCommand.execute(update, bot);

        Mockito.verify(bot)
                .execute(argThat(msg -> msg.getParameters().get("chat_id").equals(123L)
                        && msg.getParameters().get("text").equals("Тег удален успешно.")));
        assertThat(dialogService.getDialog(123L)).isNull();
    }

    @Test
    void shouldProcessTagInputAndCallRemoveTagFailure() {
        when(message.text()).thenReturn("/removetag");
        removeTagCommand.execute(update, bot);
        when(message.text()).thenReturn("http://example.com");
        removeTagCommand.execute(update, bot);
        when(message.text()).thenReturn("tag1");
        when(commandService.removeTagFromTrackedLink(123L, "http://example.com", "tag1"))
                .thenReturn(Mono.just(false));
        removeTagCommand.execute(update, bot);

        Mockito.verify(bot)
                .execute(argThat(msg -> msg.getParameters().get("chat_id").equals(123L)
                        && msg.getParameters().get("text").equals("Ошибка! Тег не удален.")));
        assertThat(dialogService.getDialog(123L)).isNull();
    }
}
