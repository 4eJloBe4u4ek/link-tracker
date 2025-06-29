package backend.academy.bot.command.command;

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
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

class AddFilterCommandTest {
    private TelegramBot bot;
    private CommandService commandService;
    private DialogService dialogService;
    private AddFilterCommand addFilterCommand;
    private Update update;
    private Message message;
    private Chat chat;

    @BeforeEach
    void setUp() {
        bot = mock(TelegramBot.class);
        commandService = mock(CommandService.class);
        dialogService = new DialogService();
        addFilterCommand = new AddFilterCommand(commandService, dialogService);

        update = mock(Update.class);
        message = mock(Message.class);
        chat = mock(Chat.class);

        when(update.message()).thenReturn(message);
        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(123L);
    }

    @Test
    void shouldReturnUsageErrorForExtraArguments() {
        when(message.text()).thenReturn("/addfilter extra");
        addFilterCommand.execute(update, bot);

        Mockito.verify(bot)
                .execute(argThat(msg -> msg.getParameters().get("chat_id").equals(123L)
                        && msg.getParameters().get("text").equals("Использование: /addfilter")));
    }

    @Test
    void shouldStartAddFilterDialog() {
        when(message.text()).thenReturn("/addfilter");
        AssertionsForClassTypes.assertThat(dialogService.getDialog(123L)).isNull();

        addFilterCommand.execute(update, bot);

        TrackingContext context = dialogService.getDialog(123L);
        AssertionsForClassTypes.assertThat(context).isNotNull();
        AssertionsForClassTypes.assertThat(context.dialogType()).isEqualTo(DialogType.ADD_FILTER);
        AssertionsForClassTypes.assertThat(context.trackState()).isEqualTo(TrackState.AWAITING_URL);

        Mockito.verify(bot)
                .execute(Mockito.argThat(
                        msg -> msg.getParameters().get("chat_id").equals(123L)
                                && msg.getParameters().get("text").equals("Укажите ссылку для добавления фильтра.")));
    }

    @Test
    void shouldProcessValidUrlSuccessfully() {
        when(message.text()).thenReturn("/addfilter");
        addFilterCommand.execute(update, bot);

        when(message.text()).thenReturn("http://example.com");
        addFilterCommand.execute(update, bot);

        TrackingContext context = dialogService.getDialog(123L);
        AssertionsForClassTypes.assertThat(context.url()).isEqualTo("http://example.com");
        AssertionsForClassTypes.assertThat(context.trackState()).isEqualTo(TrackState.AWAITING_FILTER);

        Mockito.verify(bot)
                .execute(Mockito.argThat(
                        msg -> msg.getParameters().get("chat_id").equals(123L)
                                && msg.getParameters().get("text").equals("Укажите фильтр для добавления к ссылке.")));
    }

    @Test
    void shouldProcessInvalidFilterCount() {
        when(message.text()).thenReturn("/addfilter");
        addFilterCommand.execute(update, bot);
        when(message.text()).thenReturn("http://example.com");
        addFilterCommand.execute(update, bot);

        when(message.text()).thenReturn("filter1 filter2");
        addFilterCommand.execute(update, bot);

        Mockito.verify(bot)
                .execute(Mockito.argThat(msg -> msg.getParameters()
                                .get("chat_id")
                                .equals(123L)
                        && msg.getParameters().get("text").equals("Введите один фильтр для добавления к ссылке.")));
        TrackingContext context = dialogService.getDialog(123L);
        AssertionsForClassTypes.assertThat(context).isNotNull();
        AssertionsForClassTypes.assertThat(context.trackState()).isEqualTo(TrackState.AWAITING_FILTER);
    }

    @Test
    void shouldProcessFilterInputAndCallAddFilterSuccessfully() {
        when(message.text()).thenReturn("/addfilter");
        addFilterCommand.execute(update, bot);
        when(message.text()).thenReturn("http://example.com");
        addFilterCommand.execute(update, bot);

        when(message.text()).thenReturn("filter1");
        when(commandService.addFilterToTrackedLink(123L, "http://example.com", "filter1"))
                .thenReturn(Mono.just(true));

        addFilterCommand.execute(update, bot);

        Mockito.verify(bot)
                .execute(Mockito.argThat(
                        msg -> msg.getParameters().get("chat_id").equals(123L)
                                && msg.getParameters().get("text").equals("Фильтр добавлен успешно.")));
        AssertionsForClassTypes.assertThat(dialogService.getDialog(123L)).isNull();
    }

    @Test
    void shouldProcessFilterInputAndCallAddFilterFailure() {
        when(message.text()).thenReturn("/addfilter");
        addFilterCommand.execute(update, bot);
        when(message.text()).thenReturn("http://example.com");
        addFilterCommand.execute(update, bot);

        when(message.text()).thenReturn("filter1");
        when(commandService.addFilterToTrackedLink(123L, "http://example.com", "filter1"))
                .thenReturn(Mono.just(false));

        addFilterCommand.execute(update, bot);

        Mockito.verify(bot)
                .execute(Mockito.argThat(
                        msg -> msg.getParameters().get("chat_id").equals(123L)
                                && msg.getParameters().get("text").equals("Ошибка! Фильтр не добавлен.")));
        AssertionsForClassTypes.assertThat(dialogService.getDialog(123L)).isNull();
    }
}
