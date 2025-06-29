package backend.academy.bot.command.command;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BotCommandInfo {
    ADD_FILTER("/addfilter", "Добавление фильтра к отслеживаемой ссылке."),
    ADD_TAG("/addtag", "Добавление тега к отслеживаемой ссылке."),
    HELP("/help", "Список доступных команд."),
    LINKS_BY_TAG("/linksbytag", "Список отслеживаемых ссылок по тегу."),
    LIST("/list", "Список отслеживаемых ссылок."),
    REMOVE_FILTER("/removefilter", "Удалить фильтр у отслеживаемой ссылки."),
    REMOVE_TAG("/removetag", "Удалить тег у отслеживаемой ссылки."),
    START("/start", "Регистрация в боте."),
    TRACK("/track", "Начать отслеживание ссылки."),
    UNTRACK("/untrack", "Прекратить отслеживание ссылки."),
    SET_MODE("/setmode", "Настройка режима уведомлений.");

    private final String commandName;
    private final String commandDescription;
}
