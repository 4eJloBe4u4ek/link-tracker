package backend.academy.shared.dto;

import java.time.LocalTime;

public record UpdateNotificationModeRequest(NotificationMode mode, LocalTime digestTime) {}
