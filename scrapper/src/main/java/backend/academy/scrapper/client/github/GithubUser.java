package backend.academy.scrapper.client.github;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GithubUser(@JsonProperty("id") Long id, @JsonProperty("login") String login) {}
