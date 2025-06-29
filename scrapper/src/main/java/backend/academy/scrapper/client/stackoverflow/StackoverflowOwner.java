package backend.academy.scrapper.client.stackoverflow;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StackoverflowOwner(
        @JsonProperty("account_id") Long accountId,
        @JsonProperty("display_name") String displayName,
        @JsonProperty("reputation") Long reputation,
        @JsonProperty("user_id") Long userId) {}
