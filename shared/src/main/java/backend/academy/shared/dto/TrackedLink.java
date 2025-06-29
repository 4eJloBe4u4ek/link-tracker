package backend.academy.shared.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class TrackedLink {
    private Long id;
    private String url;
    private List<String> tags;
    private List<String> filters;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
