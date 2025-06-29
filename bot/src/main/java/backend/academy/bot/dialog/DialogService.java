package backend.academy.bot.dialog;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class DialogService {
    private final Map<Long, TrackingContext> dialogs = new ConcurrentHashMap<>();

    public void startDialog(Long chatId, DialogType dialogType) {
        dialogs.put(chatId, new TrackingContext(chatId, dialogType));
    }

    public TrackingContext getDialog(Long chatId) {
        return dialogs.get(chatId);
    }

    public void endDialog(Long chatId) {
        dialogs.remove(chatId);
    }
}
