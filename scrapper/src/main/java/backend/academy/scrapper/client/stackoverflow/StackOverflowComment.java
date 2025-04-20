package backend.academy.scrapper.client.stackoverflow;

import backend.academy.shared.deserializer.UnixToLocalDateTimeDeserializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.time.LocalDateTime;

public record StackOverflowComment(
        @JsonProperty("comment_id") Long commentId,
        @JsonProperty("post_id") Long postId,
        @JsonProperty("owner") StackOverflowOwner owner,
        @JsonProperty("body") String body,
        @JsonProperty("creation_date") @JsonDeserialize(using = UnixToLocalDateTimeDeserializer.class)
                LocalDateTime creationDate) {}
