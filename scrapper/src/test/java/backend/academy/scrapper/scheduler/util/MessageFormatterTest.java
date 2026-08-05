package backend.academy.scrapper.scheduler.util;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import backend.academy.scrapper.client.github.GithubComment;
import backend.academy.scrapper.client.github.GithubCommit;
import backend.academy.scrapper.client.github.GithubIssue;
import backend.academy.scrapper.client.github.GithubPullRequest;
import backend.academy.scrapper.client.github.GithubUser;
import backend.academy.scrapper.client.puppettheatre.PuppetTheatreSession;
import backend.academy.scrapper.client.stackoverflow.StackOverflowAnswer;
import backend.academy.scrapper.client.stackoverflow.StackOverflowComment;
import backend.academy.scrapper.client.stackoverflow.StackOverflowOwner;
import backend.academy.scrapper.client.stackoverflow.StackOverflowQuestion;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class MessageFormatterTest {
    @Test
    void shouldFormatGithubIssueCorrectly() {
        // Arrange
        GithubIssue issue = mock(GithubIssue.class);
        when(issue.title()).thenReturn("Issue Title");
        when(issue.user()).thenReturn(new GithubUser(12345L, "TestUser"));
        LocalDateTime now = LocalDateTime.now();
        when(issue.createdAt()).thenReturn(now);
        when(issue.body()).thenReturn("Issue Body");

        // Act
        String result = MessageFormatter.formatGithubIssue(issue);

        // Assert
        assertThat(result)
                .contains("Название: Issue Title")
                .contains("Пользователь: TestUser")
                .contains("Время создания: " + now.toLocalTime().withNano(0))
                .contains("Описание: Issue Body");
    }

    @Test
    void shouldFormatGithubCommitCorrectly() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        GithubCommit.Author author = new GithubCommit.Author(12345L, "TestUser", now);
        GithubCommit commit = new GithubCommit(new GithubCommit.Commit(author, "Commit url", "Commit message"));

        // Act
        String result = MessageFormatter.formatGithubCommit(commit);

        // Assert
        assertThat(result)
                .contains("Пользователь: TestUser")
                .contains("Время создания: " + now.toLocalTime().withNano(0))
                .contains("Описание: Commit message");
    }

    @Test
    void shouldFormatGithubCommentCorrectly() {
        // Arrange
        GithubComment comment = mock(GithubComment.class);
        when(comment.user()).thenReturn(new GithubUser(12345L, "TestUser"));
        LocalDateTime now = LocalDateTime.now();
        when(comment.createdAt()).thenReturn(now);
        when(comment.body()).thenReturn("Comment body");

        // Act
        String result = MessageFormatter.formatGithubComment(comment);

        // Assert
        assertThat(result)
                .contains("Пользователь: TestUser")
                .contains("Время создания: " + now.toLocalTime().withNano(0))
                .contains("Описание: Comment body");
    }

    @Test
    void shouldFormatGithubPullRequestCorrectly() {
        // Arrange
        GithubPullRequest pullRequest = mock(GithubPullRequest.class);
        LocalDateTime now = LocalDateTime.now();
        when(pullRequest.title()).thenReturn("PR Title");
        when(pullRequest.user()).thenReturn(new GithubUser(12345L, "TestUser"));
        when(pullRequest.createdAt()).thenReturn(now);
        when(pullRequest.body()).thenReturn("Pull request description");

        // Act
        String result = MessageFormatter.formatGithubPullRequest(pullRequest);

        // Assert
        assertThat(result)
                .contains("Название: PR Title")
                .contains("Пользователь: TestUser")
                .contains("Время создания: " + now.toLocalTime().withNano(0))
                .contains("Описание: Pull request description");
    }

    @Test
    void shouldFormatStackoverflowQuestionCorrectly() {
        // Arrange
        StackOverflowQuestion question = mock(StackOverflowQuestion.class);
        LocalDateTime now = LocalDateTime.now();
        when(question.title()).thenReturn("Stackoverflow Question");
        when(question.owner()).thenReturn(new StackOverflowOwner(12345L, "TestUser", 100L, 12345L));
        when(question.lastActivityDate()).thenReturn(now);

        // Act
        String result = MessageFormatter.formatStackoverflowQuestion(question);

        // Assert
        assertThat(result)
                .contains("Тема вопроса: Stackoverflow Question")
                .contains("Пользователь: TestUser")
                .contains("Время обновления: " + now.toLocalTime().withNano(0));
    }

    @Test
    void shouldFormatStackoverflowAnswerCorrectly() {
        // Arrange
        StackOverflowQuestion question = mock(StackOverflowQuestion.class);
        when(question.title()).thenReturn("Stackoverflow Question");
        StackOverflowAnswer answer = mock(StackOverflowAnswer.class);
        LocalDateTime now = LocalDateTime.now();
        when(answer.owner()).thenReturn(new StackOverflowOwner(12345L, "TestUser", 100L, 12345L));
        when(answer.creationDate()).thenReturn(now);
        when(answer.body()).thenReturn("Answer text");

        // Act
        String result = MessageFormatter.formatStackoverflowAnswer(question, answer);

        // Assert
        assertThat(result)
                .contains("Тема вопроса: Stackoverflow Question")
                .contains("Пользователь: TestUser")
                .contains("Время создания: " + now.toLocalTime().withNano(0))
                .contains("Описание: Answer text");
    }

    @Test
    void shouldFormatStackoverflowCommentCorrectly() {
        // Arrange
        StackOverflowQuestion question = mock(StackOverflowQuestion.class);
        when(question.title()).thenReturn("Stackoverflow Question");
        StackOverflowComment comment = mock(StackOverflowComment.class);
        LocalDateTime now = LocalDateTime.now();
        when(comment.owner()).thenReturn(new StackOverflowOwner(12345L, "TestUser", 100L, 12345L));
        when(comment.creationDate()).thenReturn(now);
        when(comment.body()).thenReturn("Comment text");

        // Act
        String result = MessageFormatter.formatStackoverflowComment(question, comment);

        // Assert
        assertThat(result)
                .contains("Тема вопроса: Stackoverflow Question")
                .contains("Пользователь: TestUser")
                .contains("Время создания: " + now.toLocalTime().withNano(0))
                .contains("Описание: Comment text");
    }

    @Test
    void shouldFormatPuppetTheatreTicketsFromTicketpro() {
        // Arrange
        PuppetTheatreSession session = new PuppetTheatreSession(
                "Кот в сапогах",
                "12.09.2026–13.09.2026",
                "11:00",
                "38–40 BYN",
                "https://www.ticketpro.by/bilety-v-teatr/kot-v-sapogah/");

        // Act
        String result = MessageFormatter.formatPuppetTheatreTickets(List.of(session));

        // Assert
        assertThat(result)
                .contains("Кот в сапогах")
                .contains("12.09.2026–13.09.2026, 11:00")
                .contains("38–40 BYN")
                .contains(session.ticketUrl())
                .contains("https://puppet-minsk.by/afisha");
    }

    @Test
    void shouldKeepPuppetTheatreDescriptionWithinReservedLimit() {
        // Arrange
        PuppetTheatreSession session = new PuppetTheatreSession(
                "Я".repeat(3860),
                "12.09.2026",
                "11:00",
                "38–40 BYN",
                "https://www.ticketpro.by/bilety-v-teatr/near-limit");

        // Act
        String result = MessageFormatter.formatPuppetTheatreTickets(List.of(session));

        // Assert
        assertThat(result.length()).isLessThanOrEqualTo(4000);
        assertThat(result).endsWith("Афиша: https://puppet-minsk.by/afisha");
    }

    @Test
    void shouldKeepPuppetTheatreMessageWithinTelegramLimit() {
        // Arrange
        List<PuppetTheatreSession> sessions = IntStream.range(0, 10)
                .mapToObj(index -> new PuppetTheatreSession(
                        "Спектакль " + index + " " + "Я".repeat(700),
                        "12.09.2026",
                        "11:00",
                        "38–40 BYN",
                        "https://www.ticketpro.by/bilety-v-teatr/" + "x".repeat(300) + index))
                .toList();

        // Act
        String result = MessageFormatter.formatPuppetTheatreTickets(sessions);

        // Assert
        assertThat(result.length()).isLessThanOrEqualTo(4096);
        assertThat(result).contains("И ещё новых сеансов: 7");
        assertThat(result).endsWith("Афиша: https://puppet-minsk.by/afisha");
    }

    @Test
    void shouldKeepPuppetTheatreHeaderAndTailWhenNoSessionBlockFits() {
        // Arrange
        List<PuppetTheatreSession> sessions = IntStream.range(0, 10)
                .mapToObj(index -> new PuppetTheatreSession(
                        "Спектакль " + index + " " + "Я".repeat(5000),
                        "12.09.2026",
                        "11:00",
                        "38–40 BYN",
                        "https://www.ticketpro.by/bilety-v-teatr/" + index))
                .toList();

        // Act
        String result = MessageFormatter.formatPuppetTheatreTickets(sessions);

        // Assert
        assertThat(result.length()).isLessThanOrEqualTo(4096);
        assertThat(result).contains("И ещё новых сеансов: 10").endsWith("Афиша: https://puppet-minsk.by/afisha");
    }

    @Test
    void shouldLimitPuppetTheatreMessageToFirstTenSessions() {
        // Arrange
        List<PuppetTheatreSession> sessions = IntStream.range(0, 11)
                .mapToObj(index -> new PuppetTheatreSession(
                        "Спектакль " + index,
                        "12.09.2026",
                        "11:00",
                        "38–40 BYN",
                        "https://www.ticketpro.by/bilety-v-teatr/" + index))
                .toList();

        // Act
        String result = MessageFormatter.formatPuppetTheatreTickets(sessions);

        // Assert
        assertThat(result)
                .contains(
                        "• Спектакль 0\n12.09.2026, 11:00\n38–40 BYN\nhttps://www.ticketpro.by/bilety-v-teatr/0\n\n",
                        "• Спектакль 1\n12.09.2026, 11:00\n38–40 BYN\nhttps://www.ticketpro.by/bilety-v-teatr/1\n\n",
                        "• Спектакль 2\n12.09.2026, 11:00\n38–40 BYN\nhttps://www.ticketpro.by/bilety-v-teatr/2\n\n",
                        "• Спектакль 3\n12.09.2026, 11:00\n38–40 BYN\nhttps://www.ticketpro.by/bilety-v-teatr/3\n\n",
                        "• Спектакль 4\n12.09.2026, 11:00\n38–40 BYN\nhttps://www.ticketpro.by/bilety-v-teatr/4\n\n",
                        "• Спектакль 5\n12.09.2026, 11:00\n38–40 BYN\nhttps://www.ticketpro.by/bilety-v-teatr/5\n\n",
                        "• Спектакль 6\n12.09.2026, 11:00\n38–40 BYN\nhttps://www.ticketpro.by/bilety-v-teatr/6\n\n",
                        "• Спектакль 7\n12.09.2026, 11:00\n38–40 BYN\nhttps://www.ticketpro.by/bilety-v-teatr/7\n\n",
                        "• Спектакль 8\n12.09.2026, 11:00\n38–40 BYN\nhttps://www.ticketpro.by/bilety-v-teatr/8\n\n",
                        "• Спектакль 9\n12.09.2026, 11:00\n38–40 BYN\nhttps://www.ticketpro.by/bilety-v-teatr/9\n\n")
                .doesNotContain(
                        "• Спектакль 10\n12.09.2026, 11:00\n38–40 BYN\nhttps://www.ticketpro.by/bilety-v-teatr/10\n\n")
                .contains("И ещё новых сеансов: 1\n\nАфиша: https://puppet-minsk.by/afisha");
    }

    @Test
    void shouldKeepShortPuppetTheatreSessionAfterLongOneDoesNotFit() {
        // Arrange
        PuppetTheatreSession longSession = new PuppetTheatreSession(
                "Длинный спектакль " + "Я".repeat(5000),
                "12.09.2026",
                "11:00",
                "38–40 BYN",
                "https://www.ticketpro.by/bilety-v-teatr/long");
        PuppetTheatreSession shortSession = new PuppetTheatreSession(
                "Короткий спектакль",
                "12.09.2026",
                "11:00",
                "38–40 BYN",
                "https://www.ticketpro.by/bilety-v-teatr/short");

        // Act
        String result = MessageFormatter.formatPuppetTheatreTickets(List.of(longSession, shortSession));

        // Assert
        assertThat(result.length()).isLessThanOrEqualTo(4096);
        assertThat(result)
                .contains(shortSession.title())
                .doesNotContain(longSession.title())
                .contains("И ещё новых сеансов: 1")
                .endsWith("Афиша: https://puppet-minsk.by/afisha");
    }
}
