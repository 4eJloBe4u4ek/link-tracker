package backend.academy.shared.dto;

import java.util.List;

public record ListLinksResponse(List<LinkResponse> links, int size) {}
