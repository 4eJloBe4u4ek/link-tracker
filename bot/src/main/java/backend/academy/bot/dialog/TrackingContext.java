package backend.academy.bot.dialog;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class TrackingContext {
    private final Long chatId;
    private final DialogType dialogType;
    private String url;
    private List<String> tags = new ArrayList<>();
    private List<String> filters = new ArrayList<>();
    private TrackState trackState;
}
