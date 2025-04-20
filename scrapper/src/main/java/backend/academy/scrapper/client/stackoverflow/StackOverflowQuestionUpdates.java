package backend.academy.scrapper.client.stackoverflow;

import java.util.List;

public record StackOverflowQuestionUpdates(
        StackOverflowQuestion question,
        List<StackOverflowAnswer> answers,
        List<StackOverflowComment> commentsToQuestion,
        List<StackOverflowComment> commentsToAnswers) {}
