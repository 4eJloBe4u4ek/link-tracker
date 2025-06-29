package backend.academy.shared.dto;

import java.time.LocalTime;

public record RegisterChatRequest(NotificationMode mode, LocalTime digestTime) {}
