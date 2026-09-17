package backend.academy.scrapper.scheduler.util;

import backend.academy.scrapper.client.github.GithubComment;
import backend.academy.scrapper.client.github.GithubCommit;
import backend.academy.scrapper.client.github.GithubIssue;
import backend.academy.scrapper.client.github.GithubPullRequest;
import backend.academy.scrapper.client.puppettheatre.PuppetTheatreSession;
import backend.academy.scrapper.client.stackoverflow.StackOverflowAnswer;
import backend.academy.scrapper.client.stackoverflow.StackOverflowComment;
import backend.academy.scrapper.client.stackoverflow.StackOverflowQuestion;
import backend.academy.scrapper.client.ticketpro.TicketproEvent;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class MessageFormatter {
    private static final int DEFAULT_TRUNCATE_LENGTH = 200;
    private static final int MAX_ITEMS_IN_MESSAGE = 10;
    private static final int MAX_DESCRIPTION_LENGTH = 3800;
    private static final String TICKETPRO_HEADER = "🎟 В продаже появились билеты\n\nПлощадка: %s\n\n";
    private static final String PUPPET_THEATRE_HEADER = "🎭 В театре кукол появились новые сеансы\n\n";
    private static final DateTimeFormatter PUPPET_THEATRE_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

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
        int maxVenueNameLength =
                MAX_DESCRIPTION_LENGTH - TICKETPRO_HEADER.formatted("").length() - initialTail.length();
        String truncatedVenueName = truncate(venueName, Math.max(0, maxVenueNameLength));
        StringBuilder message = new StringBuilder(TICKETPRO_HEADER.formatted(truncatedVenueName));

        int includedEvents = 0;
        for (TicketproEvent event : events) {
            if (includedEvents >= MAX_ITEMS_IN_MESSAGE) {
                continue;
            }
            String eventBlock = formatTicketproEvent(event);
            String tail = formatTicketproTail(events.size() - includedEvents - 1);
            if (message.length() + eventBlock.length() + tail.length() > MAX_DESCRIPTION_LENGTH) {
                continue;
            }
            message.append(eventBlock);
            includedEvents++;
        }
        return message.append(formatTicketproTail(events.size() - includedEvents))
                .toString();
    }

    public static String formatPuppetTheatreSessions(List<PuppetTheatreSession> sessions) {
        StringBuilder message = new StringBuilder(PUPPET_THEATRE_HEADER);
        int includedSessions = 0;
        for (PuppetTheatreSession session : sessions) {
            if (includedSessions >= MAX_ITEMS_IN_MESSAGE) {
                continue;
            }
            String sessionBlock = formatPuppetTheatreSession(session);
            String tail = formatPuppetTheatreTail(sessions.size() - includedSessions - 1);
            if (message.length() + sessionBlock.length() + tail.length() > MAX_DESCRIPTION_LENGTH) {
                continue;
            }
            message.append(sessionBlock);
            includedSessions++;
        }
        return message.append(formatPuppetTheatreTail(sessions.size() - includedSessions))
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

    private static String formatPuppetTheatreSession(PuppetTheatreSession session) {
        return "• " + session.title() + '\n'
                + session.date().format(PUPPET_THEATRE_DATE_FORMATTER) + ", " + session.time() + '\n'
                + session.ticketUrl() + "\n\n";
    }

    private static String formatPuppetTheatreTail(int omittedSessions) {
        return omittedSessions == 0 ? "" : "И ещё новых сеансов: " + omittedSessions;
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
