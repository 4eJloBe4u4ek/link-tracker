package backend.academy.scrapper.client.stackoverflow;

import backend.academy.shared.deserializer.UnixToLocalDateTimeDeserializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.time.LocalDateTime;

public record StackoverflowQuestion(
        @JsonProperty("question_id") Long questionId,
        @JsonProperty("owner") StackoverflowOwner owner,
        @JsonProperty("creation_date") @JsonDeserialize(using = UnixToLocalDateTimeDeserializer.class)
                LocalDateTime creationDate,
        @JsonProperty("last_activity_date") @JsonDeserialize(using = UnixToLocalDateTimeDeserializer.class)
                LocalDateTime lastActivityDate,
        @JsonProperty("last_edit_date") @JsonDeserialize(using = UnixToLocalDateTimeDeserializer.class)
                LocalDateTime lastEditDate,
        @JsonProperty("title") String title,
        @JsonProperty("is_answered") Boolean isAnswered,
        @JsonProperty("answer_count") int answerCount) {}
