package backend.academy.scrapper.client.github;

import backend.academy.shared.deserializer.UtcToSystemDefaultDeserializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.time.LocalDateTime;

public record GithubComment(
        @JsonProperty("id") Long id,
        @JsonProperty("body") String body,
        @JsonProperty("user") GithubUser user,
        @JsonProperty("created_at") @JsonDeserialize(using = UtcToSystemDefaultDeserializer.class)
                LocalDateTime createdAt,
        @JsonProperty("updated_at") @JsonDeserialize(using = UtcToSystemDefaultDeserializer.class)
                LocalDateTime updatedAt) {}
