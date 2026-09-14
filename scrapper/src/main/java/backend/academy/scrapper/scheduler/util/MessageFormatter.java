package backend.academy.scrapper.scheduler.util;

import backend.academy.scrapper.client.github.GithubComment;
import backend.academy.scrapper.client.github.GithubCommit;
import backend.academy.scrapper.client.github.GithubIssue;
import backend.academy.scrapper.client.github.GithubPullRequest;
import backend.academy.scrapper.client.ticketpro.TicketproEvent;
import backend.academy.scrapper.client.stackoverflow.StackOverflowAnswer;
import backend.academy.scrapper.client.stackoverflow.StackOverflowComment;
import backend.academy.scrapper.client.stackoverflow.StackOverflowQuestion;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class MessageFormatter {
    private static final int DEFAULT_TRUNCATE_LENGTH = 200;
    private static final int MAX_TICKETPRO_EVENTS_IN_MESSAGE = 10;
    private static final int MAX_TICKETPRO_DESCRIPTION_LENGTH = 3800;
    private static final String TICKETPRO_HEADER = "🎟 В продаже появились билеты\n\nПлощадка: %s\n\n";

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

    public static String formatTicketproEvents(String venueName, List<TicketproEvent> events) {
        String initialTail = formatTicketproTail(events.size());
        int maxVenueNameLength = MAX_TICKETPRO_DESCRIPTION_LENGTH
                - TICKETPRO_HEADER.formatted("").length()
                - initialTail.length();
        String truncatedVenueName = truncate(venueName, Math.max(0, maxVenueNameLength));
        StringBuilder message = new StringBuilder(TICKETPRO_HEADER.formatted(truncatedVenueName));

        int includedEvents = 0;
        for (TicketproEvent event : events) {
            if (includedEvents >= MAX_TICKETPRO_EVENTS_IN_MESSAGE) {
                continue;
            }
            String eventBlock = formatTicketproEvent(event);
            String tail = formatTicketproTail(events.size() - includedEvents - 1);
            if (message.length() + eventBlock.length() + tail.length() > MAX_TICKETPRO_DESCRIPTION_LENGTH) {
                continue;
            }
            message.append(eventBlock);
            includedEvents++;
        }
        return message.append(formatTicketproTail(events.size() - includedEvents))
                .toString();
    }

    private static String formatTicketproEvent(TicketproEvent event) {
        return "• " + event.title() + '\n'
            + event.date() + (event.time().isBlank() ? "" : ", " + event.time()) + '\n'
            + (event.price().isBlank() ? "" : event.price() + "\n")
            + event.ticketUrl() + "\n\n";
    }

    private static String formatTicketproTail(int omittedEvents) {
        return omittedEvents == 0 ? "" : "И ещё новых событий: " + omittedEvents;
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
