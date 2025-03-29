package backend.academy.scrapper.client.stackoverflow;

import java.util.List;

public record StackoverflowQuestionUpdates(
        StackoverflowQuestion question,
        List<StackoverflowAnswer> answers,
        List<StackoverflowComment> commentsToQuestion,
        List<StackoverflowComment> commentsToAnswers) {}
