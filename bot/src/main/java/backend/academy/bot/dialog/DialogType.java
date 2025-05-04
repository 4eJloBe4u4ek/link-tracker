package backend.academy.bot.dialog;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DialogType {
    START("/start"),
    TRACK("/track"),
    UNTRACK("/untrack"),
    LINKS_BY_TAG("/linksbytag"),
    ADD_TAG("/addtag"),
    REMOVE_TAG("/removetag"),
    ADD_FILTER("/addfilter"),
    REMOVE_FILTER("/removefilter"),
    UPDATE_NOTIFICATION_MODE("/setmode");

    private final String command;
}
