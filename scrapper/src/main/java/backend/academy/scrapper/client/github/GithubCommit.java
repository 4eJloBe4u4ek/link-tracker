package backend.academy.scrapper.client.github;

import backend.academy.shared.deserializer.UtcToSystemDefaultDeserializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.time.LocalDateTime;

public record GithubCommit(@JsonProperty("commit") Commit commit) {
    public record Commit(
            @JsonProperty("committer") Committer committer,
            @JsonProperty("url") String url,
            @JsonProperty("message") String message) {}

    public record Committer(
            @JsonProperty("name") String name,
            @JsonProperty("date") @JsonDeserialize(using = UtcToSystemDefaultDeserializer.class) LocalDateTime date) {}
}
