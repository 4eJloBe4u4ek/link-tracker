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

class RemoveFilterCommandTest {
    private TelegramBot bot;
    private CommandService commandService;
    private DialogService dialogService;
    private RemoveFilterCommand removeFilterCommand;
    private Update update;
    private Message message;
    private Chat chat;

    @BeforeEach
    void setUp() {
        bot = mock(TelegramBot.class);
        commandService = mock(CommandService.class);
        dialogService = new DialogService();
        removeFilterCommand = new RemoveFilterCommand(commandService, dialogService);

        update = mock(Update.class);
        message = mock(Message.class);
        chat = mock(Chat.class);

        when(update.message()).thenReturn(message);
        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(123L);
    }

    @Test
    void shouldReturnUsageErrorForExtraArguments() {
        when(message.text()).thenReturn("/removefilter extra");
        removeFilterCommand.execute(update, bot);

        Mockito.verify(bot)
                .execute(argThat(msg -> msg.getParameters().get("chat_id").equals(123L)
                        && msg.getParameters().get("text").equals("Использование: /removefilter")));
    }

    @Test
    void shouldStartRemoveFilterDialog() {
        when(message.text()).thenReturn("/removefilter");
        assertThat(dialogService.getDialog(123L)).isNull();

        removeFilterCommand.execute(update, bot);

        TrackingContext context = dialogService.getDialog(123L);
        assertThat(context).isNotNull();
        assertThat(context.dialogType()).isEqualTo(DialogType.REMOVE_FILTER);
        assertThat(context.trackState()).isEqualTo(TrackState.AWAITING_URL);

        Mockito.verify(bot)
                .execute(argThat(msg -> msg.getParameters().get("chat_id").equals(123L)
                        && msg.getParameters().get("text").equals("Укажите ссылку для удаления фильтра.")));
    }

    @Test
    void shouldProcessValidUrlAndPromptFilter() {
        when(message.text()).thenReturn("/removefilter");
        removeFilterCommand.execute(update, bot);
        when(message.text()).thenReturn("http://example.com");
        removeFilterCommand.execute(update, bot);

        TrackingContext ctx = dialogService.getDialog(123L);
        assertThat(ctx.url()).isEqualTo("http://example.com");
        assertThat(ctx.trackState()).isEqualTo(TrackState.AWAITING_FILTER);

        Mockito.verify(bot)
                .execute(argThat(msg -> msg.getParameters().get("chat_id").equals(123L)
                        && msg.getParameters().get("text").equals("Укажите фильтр для удаления.")));
    }

    @Test
    void shouldProcessInvalidFilterCount() {
        when(message.text()).thenReturn("/removefilter");
        removeFilterCommand.execute(update, bot);
        when(message.text()).thenReturn("http://example.com");
        removeFilterCommand.execute(update, bot);
        when(message.text()).thenReturn("filter1 filter2");
        removeFilterCommand.execute(update, bot);

        Mockito.verify(bot)
                .execute(argThat(msg -> msg.getParameters().get("chat_id").equals(123L)
                        && msg.getParameters().get("text").equals("Введите один фильтр для удаления у ссылки.")));

        TrackingContext context = dialogService.getDialog(123L);
        assertThat(context).isNotNull();
        assertThat(context.trackState()).isEqualTo(TrackState.AWAITING_FILTER);
    }

    @Test
    void shouldProcessFilterInputAndCallRemoveFilterSuccessfully() {
        when(message.text()).thenReturn("/removefilter");
        removeFilterCommand.execute(update, bot);
        when(message.text()).thenReturn("http://example.com");
        removeFilterCommand.execute(update, bot);
        when(message.text()).thenReturn("filter1");
        when(commandService.removeFilterFromTrackedLink(123L, "http://example.com", "filter1"))
                .thenReturn(Mono.just(true));
        removeFilterCommand.execute(update, bot);

        Mockito.verify(bot)
                .execute(argThat(msg -> msg.getParameters().get("chat_id").equals(123L)
                        && msg.getParameters().get("text").equals("Фильтр удален успешно.")));
        assertThat(dialogService.getDialog(123L)).isNull();
    }

    @Test
    void shouldProcessFilterInputAndCallRemoveFilterFailure() {
        when(message.text()).thenReturn("/removefilter");
        removeFilterCommand.execute(update, bot);
        when(message.text()).thenReturn("http://example.com");
        removeFilterCommand.execute(update, bot);
        when(message.text()).thenReturn("filter1");
        when(commandService.removeFilterFromTrackedLink(123L, "http://example.com", "filter1"))
                .thenReturn(Mono.just(false));
        removeFilterCommand.execute(update, bot);

        Mockito.verify(bot)
                .execute(argThat(msg -> msg.getParameters().get("chat_id").equals(123L)
                        && msg.getParameters().get("text").equals("Ошибка! Фильтр не удален.")));
        assertThat(dialogService.getDialog(123L)).isNull();
    }
}
