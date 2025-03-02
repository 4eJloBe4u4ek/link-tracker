package backend.academy.shared.dto;

import java.util.List;

public record LinkUpdate(Long id, String url, String description, List<Long> tgChatIds) {}
