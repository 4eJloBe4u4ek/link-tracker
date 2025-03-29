package backend.academy.scrapper.client.github;

import backend.academy.shared.deserializer.UtcToSystemDefaultDeserializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.time.LocalDateTime;

public record GithubIssue(
        @JsonProperty("id") Long id,
        @JsonProperty("title") String title,
        @JsonProperty("body") String body,
        @JsonProperty("user") GithubUser user,
        @JsonProperty("state") String state,
        @JsonProperty("created_at") @JsonDeserialize(using = UtcToSystemDefaultDeserializer.class)
                LocalDateTime createdAt,
        @JsonProperty("updated_at") @JsonDeserialize(using = UtcToSystemDefaultDeserializer.class)
                LocalDateTime updatedAt) {
    public GithubIssue(
            Long id,
            String title,
            String body,
            GithubUser user,
            String state,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.id = id;
        this.title = title;
        // todo
        this.body = body;
        this.user = user;
        this.state = state;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
