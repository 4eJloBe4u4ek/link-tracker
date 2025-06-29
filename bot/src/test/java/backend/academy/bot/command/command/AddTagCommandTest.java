package backend.academy.bot.command.command;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
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

class AddTagCommandTest {
    private TelegramBot bot;
    private CommandService commandService;
    private DialogService dialogService;
    private AddTagCommand addTagCommand;
    private Update update;
    private Message message;
    private Chat chat;

    @BeforeEach
    void setUp() {
        bot = mock(TelegramBot.class);
        commandService = mock(CommandService.class);
        dialogService = new DialogService();
        addTagCommand = new AddTagCommand(commandService, dialogService);

        update = mock(Update.class);
        message = mock(Message.class);
        chat = mock(Chat.class);

        when(update.message()).thenReturn(message);
        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(123L);
    }

    @Test
    void shouldReturnUsageErrorForExtraArguments() {
        when(message.text()).thenReturn("/addtag extra");
        addTagCommand.execute(update, bot);

        Mockito.verify(bot)
                .execute(argThat(msg -> msg.getParameters().get("chat_id").equals(123L)
                        && msg.getParameters().get("text").equals("Использование: /addtag")));
    }

    @Test
    void shouldStartAddTagDialog() {
        when(message.text()).thenReturn("/addtag");
        assertThat(dialogService.getDialog(123L)).isNull();

        addTagCommand.execute(update, bot);

        TrackingContext context = dialogService.getDialog(123L);
        assertThat(context).isNotNull();
        assertThat(context.dialogType()).isEqualTo(DialogType.ADD_TAG);
        assertThat(context.trackState()).isEqualTo(TrackState.AWAITING_URL);

        Mockito.verify(bot)
                .execute(Mockito.argThat(
                        msg -> msg.getParameters().get("chat_id").equals(123L)
                                && msg.getParameters().get("text").equals("Укажите ссылку для добавления тега.")));
    }

    @Test
    void shouldProcessValidUrlSuccessfully() {
        when(message.text()).thenReturn("/addtag");
        addTagCommand.execute(update, bot);

        when(message.text()).thenReturn("http://example.com");
        addTagCommand.execute(update, bot);

        TrackingContext context = dialogService.getDialog(123L);
        assertThat(context.url()).isEqualTo("http://example.com");
        assertThat(context.trackState()).isEqualTo(TrackState.AWAITING_TAG);

        Mockito.verify(bot)
                .execute(Mockito.argThat(
                        msg -> msg.getParameters().get("chat_id").equals(123L)
                                && msg.getParameters().get("text").equals("Укажите тег для добавления к ссылке.")));
    }

    @Test
    void shouldProcessInvalidTagCount() {
        when(message.text()).thenReturn("/addtag");
        addTagCommand.execute(update, bot);
        when(message.text()).thenReturn("http://example.com");
        addTagCommand.execute(update, bot);

        when(message.text()).thenReturn("tag1 tag2");
        addTagCommand.execute(update, bot);

        Mockito.verify(bot)
                .execute(Mockito.argThat(msg -> msg.getParameters()
                                .get("chat_id")
                                .equals(123L)
                        && msg.getParameters().get("text").equals("Введите один тег для добавления к ссылке.")));
        TrackingContext context = dialogService.getDialog(123L);
        assertThat(context).isNotNull();
        assertThat(context.trackState()).isEqualTo(TrackState.AWAITING_TAG);
    }

    @Test
    void shouldProcessTagInputAndCallAddTagSuccessfully() {
        when(message.text()).thenReturn("/addtag");
        addTagCommand.execute(update, bot);
        when(message.text()).thenReturn("http://example.com");
        addTagCommand.execute(update, bot);

        when(message.text()).thenReturn("tag1");
        when(commandService.addTagToTrackedLink(123L, "http://example.com", "tag1"))
                .thenReturn(Mono.just(true));

        addTagCommand.execute(update, bot);

        Mockito.verify(bot)
                .execute(Mockito.argThat(
                        msg -> msg.getParameters().get("chat_id").equals(123L)
                                && msg.getParameters().get("text").equals("Тег добавлен успешно.")));
        assertThat(dialogService.getDialog(123L)).isNull();
    }

    @Test
    void shouldProcessTagInputAndCallAddTagFailure() {
        when(message.text()).thenReturn("/addtag");
        addTagCommand.execute(update, bot);
        when(message.text()).thenReturn("http://example.com");
        addTagCommand.execute(update, bot);

        when(message.text()).thenReturn("tag1");
        when(commandService.addTagToTrackedLink(123L, "http://example.com", "tag1"))
                .thenReturn(Mono.just(false));

        addTagCommand.execute(update, bot);

        Mockito.verify(bot)
                .execute(Mockito.argThat(
                        msg -> msg.getParameters().get("chat_id").equals(123L)
                                && msg.getParameters().get("text").equals("Ошибка! Тег не добавлен.")));
        assertThat(dialogService.getDialog(123L)).isNull();
    }
}
