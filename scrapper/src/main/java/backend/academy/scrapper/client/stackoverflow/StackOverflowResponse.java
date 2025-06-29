package backend.academy.scrapper.client.stackoverflow;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record StackOverflowResponse<T>(@JsonProperty("items") List<T> items) {}
