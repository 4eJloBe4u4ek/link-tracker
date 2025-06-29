package backend.academy.shared.api;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class ApiEndpoints {
    public static final String UPDATES = "/updates";

    private static final String TG_CHAT = "/tg-chat";
    public static final String TG_CHAT_BY_ID = TG_CHAT + "/{id}";
    public static final String TG_CHAT_NOTIFICATION_BY_ID = TG_CHAT + "/{id}/notification";

    private static final String LINKS = "/links";
    public static final String LINKS_BY_CHAT = LINKS;
    public static final String LINKS_ADD = LINKS;
    public static final String LINKS_DELETE = LINKS;
    public static final String LINKS_BY_TAG = LINKS + "/by-tag";

    private static final String TAGS = "/tags";
    public static final String TAGS_ADD = TAGS + "/add";
    public static final String TAGS_REMOVE = TAGS + "/remove";

    private static final String FILTERS = "/filters";
    public static final String FILTERS_ADD = FILTERS + "/add";
    public static final String FILTERS_REMOVE = FILTERS + "/remove";
}
