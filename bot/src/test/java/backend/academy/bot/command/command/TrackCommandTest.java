package backend.academy.bot.command.command;

import static backend.academy.bot.TestData.CMD_TRACK;
import static backend.academy.bot.TestData.EXTRA_ARGUMENT;
import static backend.academy.bot.TestData.TELEGRAM_PARAM_CHAT_ID;
import static backend.academy.bot.TestData.TELEGRAM_PARAM_TEXT;
import static backend.academy.bot.TestData.TEST_CHAT_ID;
import static backend.academy.bot.TestData.TEST_URL;
import static backend.academy.bot.TestData.TRACK_ERROR_TRACK_LINK;
import static backend.academy.bot.TestData.TRACK_PROMPT_ENTER_FILTERS;
import static backend.academy.bot.TestData.TRACK_PROMPT_ENTER_TAGS;
import static backend.academy.bot.TestData.TRACK_PROMPT_ENTER_TRACK_URL;
import static backend.academy.bot.TestData.TRACK_SUCCESS_TRACK_LINK;
import static backend.academy.bot.TestData.USAGE_TRACK;
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
    private static final String TAGS_INPUT = "tag1 tag2";
    private static final String FILTERS_INPUT = "filter1 filter2";
    private static final List<String> EXPECTED_TAGS = List.of("tag1", "tag2");
    private static final List<String> EXPECTED_FILTERS = List.of("filter1", "filter2");

    private TelegramBot bot;
    private CommandService commandService;
    private DialogService dialogService;
    private TrackCommand trackCommand;
    private Update update;
    private Message message;

    @BeforeEach
    void setUp() {
        bot = mock(TelegramBot.class);
        commandService = mock(CommandService.class);
        dialogService = new DialogService();
        trackCommand = new TrackCommand(commandService, dialogService);

        update = mock(Update.class);
        message = mock(Message.class);
        Chat chat = mock(Chat.class);

        when(update.message()).thenReturn(message);
        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(TEST_CHAT_ID);
    }

    @Test
    void shouldReturnUsageErrorForExtraArguments() {
        when(message.text()).thenReturn(CMD_TRACK + EXTRA_ARGUMENT);

        trackCommand.execute(update, bot);

        Mockito.verify(bot)
                .execute(Mockito.argThat(
                        msg -> msg.getParameters().get(TELEGRAM_PARAM_CHAT_ID).equals(TEST_CHAT_ID)
                                && msg.getParameters().get(TELEGRAM_PARAM_TEXT).equals(USAGE_TRACK)));
    }

    @Test
    void shouldStartTrackDialog() {
        when(message.text()).thenReturn(CMD_TRACK);

        trackCommand.execute(update, bot);

        assertThat(dialogService.getDialog(TEST_CHAT_ID).dialogType()).isEqualTo(DialogType.TRACK);
        Mockito.verify(bot)
                .execute(Mockito.argThat(
                        msg -> msg.getParameters().get(TELEGRAM_PARAM_CHAT_ID).equals(TEST_CHAT_ID)
                                && msg.getParameters().get(TELEGRAM_PARAM_TEXT).equals(TRACK_PROMPT_ENTER_TRACK_URL)));
    }

    @Test
    void shouldProcessValidUrlSuccessfully() {
        when(message.text()).thenReturn(CMD_TRACK);
        trackCommand.execute(update, bot);
        when(message.text()).thenReturn(TEST_URL);
        trackCommand.execute(update, bot);

        TrackingContext context = dialogService.getDialog(TEST_CHAT_ID);
        assertThat(context.url()).isEqualTo(TEST_URL);
        assertThat(context.trackState()).isEqualTo(TrackState.AWAITING_TAGS);

        Mockito.verify(bot)
                .execute(Mockito.argThat(
                        msg -> msg.getParameters().get(TELEGRAM_PARAM_CHAT_ID).equals(TEST_CHAT_ID)
                                && msg.getParameters().get(TELEGRAM_PARAM_TEXT).equals(TRACK_PROMPT_ENTER_TAGS)));
    }

    @Test
    void shouldProcessTagsInputSuccessfully() {
        when(message.text()).thenReturn(CMD_TRACK);
        trackCommand.execute(update, bot);
        when(message.text()).thenReturn(TEST_URL);
        trackCommand.execute(update, bot);
        when(message.text()).thenReturn(TAGS_INPUT);
        trackCommand.execute(update, bot);

        TrackingContext context = dialogService.getDialog(TEST_CHAT_ID);
        List<String> tags = context.tags();
        assertThat(tags).isEqualTo(EXPECTED_TAGS);
        assertThat(context.trackState()).isEqualTo(TrackState.AWAITING_FILTERS);

        Mockito.verify(bot)
                .execute(Mockito.argThat(
                        msg -> msg.getParameters().get(TELEGRAM_PARAM_CHAT_ID).equals(TEST_CHAT_ID)
                                && msg.getParameters().get(TELEGRAM_PARAM_TEXT).equals(TRACK_PROMPT_ENTER_FILTERS)));
    }

    @Test
    void shouldProcessFiltersInputAndCallTrackLinkSuccessfully() {
        when(message.text()).thenReturn(CMD_TRACK);
        trackCommand.execute(update, bot);
        when(message.text()).thenReturn(TEST_URL);
        trackCommand.execute(update, bot);
        when(message.text()).thenReturn(TAGS_INPUT);
        trackCommand.execute(update, bot);
        when(message.text()).thenReturn(FILTERS_INPUT);
        when(commandService.trackLink(TEST_CHAT_ID, TEST_URL, EXPECTED_TAGS, EXPECTED_FILTERS))
                .thenReturn(Mono.just(true));

        trackCommand.execute(update, bot);

        Mockito.verify(bot)
                .execute(Mockito.argThat(
                        msg -> msg.getParameters().get(TELEGRAM_PARAM_CHAT_ID).equals(TEST_CHAT_ID)
                                && msg.getParameters()
                                        .get(TELEGRAM_PARAM_TEXT)
                                        .equals(String.format(TRACK_SUCCESS_TRACK_LINK, TEST_URL))));
        assertThat(dialogService.getDialog(TEST_CHAT_ID)).isNull();
    }

    @Test
    void shouldProcessFiltersInputAndCallTrackLinkFailure() {
        when(message.text()).thenReturn(CMD_TRACK);
        trackCommand.execute(update, bot);
        when(message.text()).thenReturn(TEST_URL);
        trackCommand.execute(update, bot);
        when(message.text()).thenReturn(TAGS_INPUT);
        trackCommand.execute(update, bot);
        when(message.text()).thenReturn(FILTERS_INPUT);
        when(commandService.trackLink(TEST_CHAT_ID, TEST_URL, EXPECTED_TAGS, EXPECTED_FILTERS))
                .thenReturn(Mono.just(false));

        trackCommand.execute(update, bot);

        Mockito.verify(bot)
                .execute(Mockito.argThat(
                        msg -> msg.getParameters().get(TELEGRAM_PARAM_CHAT_ID).equals(TEST_CHAT_ID)
                                && msg.getParameters()
                                        .get(TELEGRAM_PARAM_TEXT)
                                        .equals(String.format(TRACK_ERROR_TRACK_LINK, TEST_URL))));
        assertThat(dialogService.getDialog(TEST_CHAT_ID)).isNull();
    }
}
