package backend.academy.scrapper.scheduler.util;

import backend.academy.scrapper.client.github.GithubComment;
import backend.academy.scrapper.client.github.GithubCommit;
import backend.academy.scrapper.client.github.GithubIssue;
import backend.academy.scrapper.client.github.GithubPullRequest;
import backend.academy.scrapper.client.stackoverflow.StackoverflowAnswer;
import backend.academy.scrapper.client.stackoverflow.StackoverflowComment;
import backend.academy.scrapper.client.stackoverflow.StackoverflowQuestion;
import lombok.experimental.UtilityClass;

@UtilityClass
public class MessageFormatter {
    private static final int DEFAULT_TRUNCATE_LENGTH = 200;

    public static String formatGithubIssue(GithubIssue githubIssue) {
        return "Название: " + githubIssue.title() + "\n"
                + "Пользователь: " + githubIssue.user().login() + "\n"
                + "Время создания: " + githubIssue.createdAt().toLocalTime() + "\n"
                + "Описание: "
                + truncate(githubIssue.body(), DEFAULT_TRUNCATE_LENGTH);
    }

    public static String formatGithubCommit(GithubCommit githubCommit) {
        return "Пользователь: " + githubCommit.commit().author().name() + "\n"
                + "Время создания: " + githubCommit.commit().author().date().toLocalTime() + "\n"
                + "Описание: "
                + truncate(githubCommit.commit().message(), DEFAULT_TRUNCATE_LENGTH);
    }

    public static String formatGithubComment(GithubComment githubComment) {
        return "Пользователь: " + githubComment.user().login() + "\n"
                + "Время создания: " + githubComment.createdAt().toLocalTime() + "\n"
                + "Описание: "
                + truncate(githubComment.body(), DEFAULT_TRUNCATE_LENGTH);
    }

    public static String formatGithubPullRequest(GithubPullRequest githubPullRequest) {
        return "Название: " + githubPullRequest.title() + "\n"
                + "Пользователь: " + githubPullRequest.user().login() + "\n"
                + "Время создания: " + githubPullRequest.createdAt().toLocalTime() + "\n"
                + "Описание: "
                + truncate(githubPullRequest.body(), DEFAULT_TRUNCATE_LENGTH);
    }

    public static String formatStackoverflowQuestion(StackoverflowQuestion question) {
        return "Тема вопроса: " + question.title() + "\n"
                + "Пользователь: " + question.owner().displayName() + "\n"
                + "Время обновления: " + question.lastActivityDate().toLocalTime();
    }

    public static String formatStackoverflowAnswer(StackoverflowQuestion question, StackoverflowAnswer answer) {
        return "Тема вопроса: " + question.title() + "\n"
                + "Пользователь: " + answer.owner().displayName() + "\n"
                + "Время создания: " + answer.creationDate().toLocalTime() + "\n"
                + "Описание: "
                + truncate(answer.body(), DEFAULT_TRUNCATE_LENGTH);
    }

    public static String formatStackoverflowComment(StackoverflowQuestion question, StackoverflowComment comment) {
        return "Тема вопроса: " + question.title() + "\n"
                + "Пользователь: " + comment.owner().displayName() + "\n"
                + "Время создания: " + comment.creationDate().toLocalTime() + "\n"
                + "Описание: "
                + truncate(comment.body(), DEFAULT_TRUNCATE_LENGTH);
    }

    private static String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        return text.substring(0, Math.min(maxLength, text.length()));
    }
}
