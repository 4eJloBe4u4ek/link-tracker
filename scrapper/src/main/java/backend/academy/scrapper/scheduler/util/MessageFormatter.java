package backend.academy.scrapper.scheduler.util;

import backend.academy.scrapper.client.github.GithubComment;
import backend.academy.scrapper.client.github.GithubCommit;
import backend.academy.scrapper.client.github.GithubIssue;
import backend.academy.scrapper.client.github.GithubPullRequest;
import backend.academy.scrapper.client.puppettheatre.PuppetTheatreSession;
import backend.academy.scrapper.client.stackoverflow.StackOverflowAnswer;
import backend.academy.scrapper.client.stackoverflow.StackOverflowComment;
import backend.academy.scrapper.client.stackoverflow.StackOverflowQuestion;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class MessageFormatter {
    private static final int DEFAULT_TRUNCATE_LENGTH = 200;
    private static final int MAX_THEATRE_SESSIONS_IN_MESSAGE = 10;
    private static final int MAX_THEATRE_DESCRIPTION_LENGTH = 4000;
    private static final String THEATRE_HEADER = "🎭 В продаже появились билеты в Белорусский театр кукол:\n\n";
    private static final String THEATRE_SCHEDULE = "Афиша: https://puppet-minsk.by/afisha";

    public static String formatGithubIssue(GithubIssue githubIssue) {
        return formatGithubMessage(
                githubIssue.title(),
                githubIssue.user().login(),
                githubIssue.createdAt().toLocalTime().toString(),
                githubIssue.body());
    }

    public static String formatGithubCommit(GithubCommit githubCommit) {
        return formatGithubMessage(
                null,
                githubCommit.commit().author().name(),
                githubCommit.commit().author().date().toLocalTime().toString(),
                githubCommit.commit().message());
    }

    public static String formatGithubComment(GithubComment githubComment) {
        return formatGithubMessage(
                null,
                githubComment.user().login(),
                githubComment.createdAt().toLocalTime().toString(),
                githubComment.body());
    }

    public static String formatGithubPullRequest(GithubPullRequest githubPullRequest) {
        return formatGithubMessage(
                githubPullRequest.title(),
                githubPullRequest.user().login(),
                githubPullRequest.createdAt().toLocalTime().toString(),
                githubPullRequest.body());
    }

    public static String formatStackoverflowQuestion(StackOverflowQuestion question) {
        return "Тема вопроса: " + question.title() + "\n"
                + "Пользователь: " + question.owner().displayName() + "\n"
                + "Время обновления: " + question.lastActivityDate().toLocalTime();
    }

    public static String formatStackoverflowAnswer(StackOverflowQuestion question, StackOverflowAnswer answer) {
        return formatStackoverflowMessage(
                question.title(),
                answer.owner().displayName(),
                answer.creationDate().toLocalTime().toString(),
                answer.body());
    }

    public static String formatStackoverflowComment(StackOverflowQuestion question, StackOverflowComment comment) {
        return formatStackoverflowMessage(
                question.title(),
                comment.owner().displayName(),
                comment.creationDate().toLocalTime().toString(),
                comment.body());
    }

    public static String formatPuppetTheatreTickets(List<PuppetTheatreSession> sessions) {
        StringBuilder message = new StringBuilder(THEATRE_HEADER);

        int sessionLimit = Math.min(sessions.size(), MAX_THEATRE_SESSIONS_IN_MESSAGE);
        int includedSessions = 0;
        for (int index = 0; index < sessionLimit; index++) {
            String sessionBlock = formatPuppetTheatreSession(sessions.get(index));
            String tail = formatPuppetTheatreTail(sessions.size() - includedSessions - 1);
            if (message.length() + sessionBlock.length() + tail.length() > MAX_THEATRE_DESCRIPTION_LENGTH) {
                continue;
            }
            message.append(sessionBlock);
            includedSessions++;
        }
        return message.append(formatPuppetTheatreTail(sessions.size() - includedSessions))
                .toString();
    }

    private static String formatPuppetTheatreSession(PuppetTheatreSession session) {
        return "• " + session.title() + '\n'
            + session.date() + (session.time().isBlank() ? "" : ", " + session.time()) + '\n'
            + (session.price().isBlank() ? "" : session.price() + "\n")
            + session.ticketUrl() + "\n\n";
    }

    private static String formatPuppetTheatreTail(int omittedSessions) {
        return (omittedSessions == 0 ? "" : "И ещё новых сеансов: " + omittedSessions + "\n\n") + THEATRE_SCHEDULE;
    }

    private static String formatGithubMessage(String title, String user, String time, String description) {
        return (title != null ? "Название: " + title + "\n" : "")
                + "Пользователь: " + user + "\n"
                + "Время создания: " + time + "\n"
                + "Описание: " + truncate(description, DEFAULT_TRUNCATE_LENGTH);
    }

    private static String formatStackoverflowMessage(String title, String user, String time, String description) {
        return "Тема вопроса: " + title + "\n"
                + "Пользователь: " + user + "\n"
                + "Время создания: " + time + "\n"
                + "Описание: "
                + truncate(description, DEFAULT_TRUNCATE_LENGTH);
    }

    private static String truncate(String text, int maxLength) {
        return text == null ? "" : text.substring(0, Math.min(maxLength, text.length()));
    }
}
