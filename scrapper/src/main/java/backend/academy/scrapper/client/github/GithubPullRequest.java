package backend.academy.scrapper.client.github;

import backend.academy.shared.deserializer.UtcToSystemDefaultDeserializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.time.LocalDateTime;

public record GithubPullRequest(
        @JsonProperty("id") Long id,
        @JsonProperty("state") String state,
        @JsonProperty("title") String title,
        @JsonProperty("body") String body,
        @JsonProperty("user") GithubUser user,
        // todo подумать над временем
        @JsonProperty("created_at") @JsonDeserialize(using = UtcToSystemDefaultDeserializer.class)
                LocalDateTime createdAt,
        @JsonProperty("updated_at") @JsonDeserialize(using = UtcToSystemDefaultDeserializer.class)
                LocalDateTime updatedAt) {
    public GithubPullRequest(
            Long id,
            String state,
            String title,
            String body,
            GithubUser user,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.id = id;
        this.state = state;
        this.title = title;
        // todo
        this.body = body;
        this.user = user;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
