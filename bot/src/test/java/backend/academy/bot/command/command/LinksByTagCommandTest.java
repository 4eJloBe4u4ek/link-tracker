package backend.academy.bot.command.command;

import static backend.academy.bot.TestData.CMD_LINKS_BY_TAG;
import static backend.academy.bot.TestData.EXTRA_ARGUMENT;
import static backend.academy.bot.TestData.INVALID_TAG_COUNT;
import static backend.academy.bot.TestData.LINKS_BY_TAG_EMPTY_LIST;
import static backend.academy.bot.TestData.LINKS_BY_TAG_ERROR_GETTING_LINKS;
import static backend.academy.bot.TestData.LINKS_BY_TAG_ERROR_TAG_COUNT;
import static backend.academy.bot.TestData.LINKS_BY_TAG_PROMPT_TAG;
import static backend.academy.bot.TestData.TELEGRAM_PARAM_CHAT_ID;
import static backend.academy.bot.TestData.TELEGRAM_PARAM_TEXT;
import static backend.academy.bot.TestData.TEST_CHAT_ID;
import static backend.academy.bot.TestData.USAGE_LINKS_BY_TAG;
import static backend.academy.bot.TestData.VALID_TAG_COUNT;
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

    @BeforeEach
    void setUp() {
        bot = mock(TelegramBot.class);
        commandService = mock(CommandService.class);
        dialogService = new DialogService();
        linksByTagCommand = new LinksByTagCommand(commandService, dialogService);

        update = mock(Update.class);
        message = mock(Message.class);
        Chat chat = mock(Chat.class);

        when(update.message()).thenReturn(message);
        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(TEST_CHAT_ID);
    }

    @Test
    void shouldReturnUsageErrorForExtraArguments() {
        when(message.text()).thenReturn(CMD_LINKS_BY_TAG + EXTRA_ARGUMENT);
        linksByTagCommand.execute(update, bot);

        Mockito.verify(bot)
                .execute(Mockito.argThat(
                        msg -> msg.getParameters().get(TELEGRAM_PARAM_CHAT_ID).equals(TEST_CHAT_ID)
                                && msg.getParameters().get(TELEGRAM_PARAM_TEXT).equals(USAGE_LINKS_BY_TAG)));
    }

    @Test
    void shouldStartLinksByTagDialog() {
        when(message.text()).thenReturn(CMD_LINKS_BY_TAG);
        assertThat(dialogService.getDialog(TEST_CHAT_ID)).isNull();

        linksByTagCommand.execute(update, bot);

        TrackingContext context = dialogService.getDialog(TEST_CHAT_ID);
        assertThat(context).isNotNull();
        assertThat(context.dialogType()).isEqualTo(DialogType.LINKS_BY_TAG);
        assertThat(context.trackState()).isEqualTo(TrackState.AWAITING_TAG);
        Mockito.verify(bot)
                .execute(Mockito.argThat(
                        msg -> msg.getParameters().get(TELEGRAM_PARAM_CHAT_ID).equals(TEST_CHAT_ID)
                                && msg.getParameters().get(TELEGRAM_PARAM_TEXT).equals(LINKS_BY_TAG_PROMPT_TAG)));
    }

    @Test
    void shouldProcessInvalidTagCount() {
        when(message.text()).thenReturn(CMD_LINKS_BY_TAG);
        linksByTagCommand.execute(update, bot);
        when(message.text()).thenReturn(INVALID_TAG_COUNT);
        linksByTagCommand.execute(update, bot);

        Mockito.verify(bot)
                .execute(Mockito.argThat(
                        msg -> msg.getParameters().get(TELEGRAM_PARAM_CHAT_ID).equals(TEST_CHAT_ID)
                                && msg.getParameters().get(TELEGRAM_PARAM_TEXT).equals(LINKS_BY_TAG_ERROR_TAG_COUNT)));
        TrackingContext context = dialogService.getDialog(TEST_CHAT_ID);
        assertThat(context).isNotNull();
        assertThat(context.trackState()).isEqualTo(TrackState.AWAITING_TAG);
    }

    @Test
    void shouldProcessValidTagInputEmptyList() {
        when(message.text()).thenReturn(CMD_LINKS_BY_TAG);
        linksByTagCommand.execute(update, bot);
        when(message.text()).thenReturn(VALID_TAG_COUNT);
        when(commandService.getTrackedLinksByTag(TEST_CHAT_ID, VALID_TAG_COUNT)).thenReturn(Mono.just(List.of()));

        linksByTagCommand.execute(update, bot);

        Mockito.verify(bot)
                .execute(Mockito.argThat(
                        msg -> msg.getParameters().get(TELEGRAM_PARAM_CHAT_ID).equals(TEST_CHAT_ID)
                                && msg.getParameters().get(TELEGRAM_PARAM_TEXT).equals(LINKS_BY_TAG_EMPTY_LIST)));
        assertThat(dialogService.getDialog(TEST_CHAT_ID)).isNull();
    }

    @Test
    void shouldHandleErrorWhenGettingLinks() {
        when(message.text()).thenReturn(CMD_LINKS_BY_TAG);
        linksByTagCommand.execute(update, bot);
        when(message.text()).thenReturn(VALID_TAG_COUNT);
        when(commandService.getTrackedLinksByTag(TEST_CHAT_ID, VALID_TAG_COUNT))
                .thenReturn(Mono.error(new RuntimeException("Error")));

        linksByTagCommand.execute(update, bot);

        Mockito.verify(bot)
                .execute(Mockito.argThat(msg -> msg.getParameters()
                                .get(TELEGRAM_PARAM_CHAT_ID)
                                .equals(TEST_CHAT_ID)
                        && msg.getParameters().get(TELEGRAM_PARAM_TEXT).equals(LINKS_BY_TAG_ERROR_GETTING_LINKS)));
        assertThat(dialogService.getDialog(TEST_CHAT_ID)).isNull();
    }
}
