package backend.academy.bot.dialog;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TrackingContext {
    private final Long chatId;
    private final DialogType dialogType;
    private String url;
    private List<String> tags = new ArrayList<>();
    private List<String> filters = new ArrayList<>();
    private TrackState trackState;

    public TrackingContext(Long chatId, DialogType dialogType) {
        this.chatId = chatId;
        this.dialogType = dialogType;
        this.trackState = TrackState.AWAITING_URL;
    }
}
