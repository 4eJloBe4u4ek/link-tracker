package backend.academy.bot;

import backend.academy.shared.dto.LinkUpdate;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class TestData {
    public static final Long TEST_CHAT_ID = 123L;
    public static final String TEST_URL = "http://example.com";
    public static final String EXTRA_ARGUMENT = " extra";
    public static final String TELEGRAM_PARAM_CHAT_ID = "chat_id";
    public static final String TELEGRAM_PARAM_TEXT = "text";
    public static final String INVALID_TIME = "25:99";
    public static final String VALID_TIME = "09:30";

    public static final String CMD_ADD_FILTER = "/addfilter";
    public static final String CMD_ADD_TAG = "/addtag";
    public static final String CMD_HELP = "/help";
    public static final String CMD_LINKS_BY_TAG = "/linksbytag";
    public static final String CMD_LIST = "/list";
    public static final String CMD_REMOVE_FILTER = "/removefilter";
    public static final String CMD_REMOVE_TAG = "/removetag";
    public static final String CMD_START = "/start";
    public static final String CMD_TRACK = "/track";
    public static final String CMD_UNTRACK = "/untrack";
    public static final String CMD_SET_MODE = "/setmode";

    public static final String USAGE_ADD_FILTER = "Использование: " + CMD_ADD_FILTER;
    public static final String USAGE_ADD_TAG = "Использование: " + CMD_ADD_TAG;
    public static final String USAGE_HELP = "Использование: " + CMD_HELP;
    public static final String USAGE_LINKS_BY_TAG = "Использование: " + CMD_LINKS_BY_TAG;
    public static final String USAGE_LIST = "Использование: " + CMD_LIST;
    public static final String USAGE_REMOVE_FILTER = "Использование: " + CMD_REMOVE_FILTER;
    public static final String USAGE_REMOVE_TAG = "Использование: " + CMD_REMOVE_TAG;
    public static final String USAGE_START = "Использование: " + CMD_START;
    public static final String USAGE_TRACK = "Использование: " + CMD_TRACK;
    public static final String USAGE_UNTRACK = "Использование: " + CMD_UNTRACK;
    public static final String USAGE_SET_MODE = "Использование: " + CMD_SET_MODE;

    public static final String ADD_FILTER_PROMPT_FILTER_URL = "Укажите ссылку для добавления фильтра.";
    public static final String ADD_FILTER_PROMPT_FILTER_NAME = "Укажите фильтр для добавления к ссылке.";
    public static final String ADD_FILTER_ERROR_FILTER_COUNT = "Введите один фильтр для добавления к ссылке.";
    public static final String ADD_FILTER_SUCCESS_FILTER_ADDED = "Фильтр добавлен успешно.";
    public static final String ADD_FILTER_ERROR_FILTER_ADD = "Ошибка! Фильтр не добавлен.";
    public static final String VALID_FILTER_COUNT = "filter1";
    public static final String INVALID_FILTER_COUNT = "filter1 filter2";

    public static final String ADD_TAG_PROMPT_TAG_URL = "Укажите ссылку для добавления тега.";
    public static final String ADD_TAG_PROMPT_TAG_NAME = "Укажите тег для добавления к ссылке.";
    public static final String ADD_TAG_ERROR_TAG_COUNT = "Введите один тег для добавления к ссылке.";
    public static final String ADD_TAG_SUCCESS_TAG_ADDED = "Тег добавлен успешно.";
    public static final String ADD_TAG_ERROR_TAG_ADD = "Ошибка! Тег не добавлен.";
    public static final String VALID_TAG_COUNT = "tag1";
    public static final String INVALID_TAG_COUNT = "tag1 tag2";

    public static final String LINKS_BY_TAG_PROMPT_TAG = "Укажите тег для вывода отслеживаемых ссылок.";
    public static final String LINKS_BY_TAG_ERROR_TAG_COUNT =
            "Введите один тег для вывода списка отслеживаемых ссылок.";
    public static final String LINKS_BY_TAG_EMPTY_LIST = "Список отслеживаемых ссылок по тегу пуст.";
    public static final String LINKS_BY_TAG_ERROR_GETTING_LINKS =
            "Произошла ошибка при получении отслеживаемых ссылок по тегу.";

    public static final String HELP_HEADER = "Доступные команды:";
    public static final String HELP_START_DESCRIPTION = "Запуск бота.";
    public static final String HELP_LIST_DESCRIPTION = "Отслеживаемые ссылки.";

    public static final String LIST_EMPTY_MESSAGE = "Список отслеживаемых ссылок пуст.";
    public static final String LIST_HEADER_MESSAGE = "Список отслеживаемых ссылок:";

    public static final String REMOVE_FILTER_PROMPT_FILTER_URL = "Укажите ссылку для удаления фильтра.";
    public static final String REMOVE_FILTER_PROMPT_FILTER_NAME = "Укажите фильтр для удаления.";
    public static final String REMOVE_FILTER_ERROR_FILTER_COUNT = "Введите один фильтр для удаления у ссылки.";
    public static final String REMOVE_FILTER_SUCCESS_FILTER_REMOVED = "Фильтр удален успешно.";
    public static final String REMOVE_FILTER_ERROR_TAG_REMOVE = "Ошибка! Фильтр не удален.";

    public static final String REMOVE_TAG_PROMPT_TAG_URL = "Укажите ссылку для удаления тега.";
    public static final String REMOVE_TAG_PROMPT_TAG_NAME = "Укажите тег для удаления.";
    public static final String REMOVE_TAG_ERROR_TAG_COUNT = "Введите один тег для удаления у ссылки.";
    public static final String REMOVE_TAG_SUCCESS_TAG_REMOVED = "Тег удален успешно.";
    public static final String REMOVE_TAG_ERROR_TAG_REMOVE = "Ошибка! Тег не удален.";

    public static final String START_GREETING_MESSAGE = "Привет! Я бот для отслеживания ссылок";
    public static final String START_REGISTRATION_COMPLETE = "Регистрация завершена!";
    public static final String ENTER_TIME_PROMPT = "Введите время";
    public static final String INVALID_CHOICE_MESSAGE = "Пожалуйста, введите 1 или 2.";
    public static final String INVALID_TIME_FORMAT_MESSAGE = "Неверный формат времени";

    public static final String TRACK_PROMPT_ENTER_TRACK_URL =
            "Укажите ссылку для отслеживания.\nПримеры поддерживаемых ссылок: /sources";
    public static final String TRACK_PROMPT_ENTER_TAGS = "Введите теги через пробел (опционально - /skip):";
    public static final String TRACK_PROMPT_ENTER_FILTERS = "Введите фильтры через пробел (опционально - /skip):";
    public static final String TRACK_SUCCESS_TRACK_LINK = "Ссылка %s добавлена в отслеживание.";
    public static final String TRACK_ERROR_TRACK_LINK = "Ошибка! Ссылка %s не добавлена в отслеживание.";

    public static final String UNTRACK_PROMPT_URL = "Укажите ссылку для прекращения отслеживания.";
    public static final String UNTRACK_ERROR_URL = "Ошибка! Невозможно прекратить отслеживание ссылки: ";
    public static final String UNTRACK_SUCCESS_MESSAGE = "Ссылка %s больше не отслеживается";

    public static final String SET_MODE_MODE_PROMPT = "Выберите новый режим уведомлений";
    public static final String SET_MODE_UPDATE_SUCCESS = "Режим уведомлений обновлен";

    public static final String UNKNOWN_COMMAND_ERROR_MESSAGE =
            "Неизвестная команда. Используйте /help для просмотра списка доступных команд.";
    public static final String UNKNOWN_COMMAND_INPUT = "/unknown";

    public static final String REDIS_TRACKED_LINKS_KEY_TEMPLATE = "trackedLinks:chat:%d";

    public static final String CLIENT_ERROR_RESPONSE =
            """
        {
            "description": "Exception description",
            "code": "400",
            "exceptionName": "Exception name",
            "exceptionMessage": "Exception message",
            "stacktrace": []
        }
    """;

    public static final String LINK_UPDATE_JSON =
            """
        {
            "id": 1,
            "url": "https://example.com",
            "description": "Example",
            "tgChatIds": [1, 2]
        }
    """;

    public static final String INVALID_JSON = "{invalid json}";

    public static final LinkUpdate TEST_GOOD_LINK_UPDATE =
            new LinkUpdate(1L, TEST_URL, "Description", List.of(TEST_CHAT_ID));
    public static final LinkUpdate TEST_BAD_LINK_UPDATE = new LinkUpdate(1L, TEST_URL, "Description", List.of());
}
