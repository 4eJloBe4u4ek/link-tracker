package backend.academy.bot.command;

import java.time.format.DateTimeFormatter;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Utils {
    public static final DateTimeFormatter HH_MM_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    public static final String SPACE_SPLIT_REGEX = "\\s+";
    public static final String NEW_LINE = "\n";
}
