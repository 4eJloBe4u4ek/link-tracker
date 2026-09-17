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
import backend.academy.scrapper.client.ticketpro.TicketproEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
    void shouldFormatTicketproEventsWithDynamicVenueAndAllFields() {
        // Arrange
        TicketproEvent event = new TicketproEvent(
                "Граф Монте-Кристо",
                "18.08.2026",
                "19:00",
                "от 50 BYN",
                "https://www.ticketpro.by/bilety-v-teatr/graf-monte-kristo/");

        // Act
        String result = MessageFormatter.formatTicketproEvents("ГУ Дворец Республики, Минск", List.of(event));

        // Assert
        assertThat(result)
                .isEqualTo(
                        "🎟 В продаже появились билеты\n\n"
                                + "Площадка: ГУ Дворец Республики, Минск\n\n"
                                + "• Граф Монте-Кристо\n"
                                + "18.08.2026, 19:00\n"
                                + "от 50 BYN\n"
                                + "https://www.ticketpro.by/bilety-v-teatr/graf-monte-kristo/\n\n");
    }

    @Test
    void shouldOmitBlankTicketproTimeAndPrice() {
        // Arrange
        TicketproEvent event = new TicketproEvent(
                "Лебединое озеро",
                "20.08.2026",
                "",
                "",
                "https://www.ticketpro.by/bilety-v-teatr/lebedinoe-ozero/");

        // Act
        String result = MessageFormatter.formatTicketproEvents("Большой театр Беларуси", List.of(event));

        // Assert
        assertThat(result)
                .contains("• Лебединое озеро\n20.08.2026\nhttps://www.ticketpro.by/bilety-v-teatr/lebedinoe-ozero/\n\n")
                .doesNotContain("20.08.2026,", "BYN");
    }

    @Test
    void shouldKeepTicketproDescriptionWithinReservedLimit() {
        // Arrange
        List<TicketproEvent> events = IntStream.range(0, 10)
                .mapToObj(index -> new TicketproEvent(
                        "Спектакль " + index + " " + "Я".repeat(700),
                        "12.09.2026",
                        "11:00",
                        "38–40 BYN",
                        "https://www.ticketpro.by/bilety-v-teatr/" + "x".repeat(300) + index))
                .toList();

        // Act
        String result = MessageFormatter.formatTicketproEvents("ГУ Дворец Республики, Минск", events);

        // Assert
        assertThat(result.length()).isLessThanOrEqualTo(3800);
        assertThat(result).contains("И ещё новых событий: 7");
        assertThat(result).doesNotContain("puppet-minsk.by", "Афиша:");
    }

    @Test
    void shouldKeepLongVenueDescriptionAndCompleteBotMessageWithinLimits() {
        // Arrange
        String venueName = "Очень длинное название площадки ".repeat(200);
        List<TicketproEvent> events = List.of(
                new TicketproEvent(
                        "Граф Монте-Кристо",
                        "18.08.2026",
                        "19:00",
                        "от 50 BYN",
                        "https://www.ticketpro.by/bilety-v-teatr/graf-monte-kristo/"),
                new TicketproEvent(
                        "Лебединое озеро",
                        "20.08.2026",
                        "",
                        "",
                        "https://www.ticketpro.by/bilety-v-teatr/lebedinoe-ozero/"));

        // Act
        String result = MessageFormatter.formatTicketproEvents(venueName, events);
        String completeBotMessage = "Новое обновление!\nURL: "
                + "https://www.ticketpro.by/koncertnye-ploshhadki/dvorec-respubliki/\n"
                + result;

        // Assert
        assertThat(result.length()).isLessThanOrEqualTo(3800);
        assertThat(completeBotMessage.length()).isLessThanOrEqualTo(4096);
        assertThat(result)
                .startsWith("🎟 В продаже появились билеты\n\nПлощадка: ")
                .doesNotContain("Граф Монте-Кристо", "Лебединое озеро")
                .endsWith("И ещё новых событий: 2");
    }

    @Test
    void shouldKeepTicketproHeaderAndTailWhenNoEventBlockFits() {
        // Arrange
        List<TicketproEvent> events = IntStream.range(0, 10)
                .mapToObj(index -> new TicketproEvent(
                        "Спектакль " + index + " " + "Я".repeat(5000),
                        "12.09.2026",
                        "11:00",
                        "38–40 BYN",
                        "https://www.ticketpro.by/bilety-v-teatr/" + index))
                .toList();

        // Act
        String result = MessageFormatter.formatTicketproEvents("ГУ Дворец Республики, Минск", events);

        // Assert
        assertThat(result.length()).isLessThanOrEqualTo(3800);
        assertThat(result)
                .startsWith("🎟 В продаже появились билеты\n\nПлощадка: ГУ Дворец Республики, Минск\n\n")
                .endsWith("И ещё новых событий: 10");
    }

    @Test
    void shouldLimitTicketproMessageToTenEventBlocks() {
        // Arrange
        List<TicketproEvent> events = IntStream.range(0, 12)
                .mapToObj(index -> new TicketproEvent(
                        "Спектакль " + index,
                        "12.09.2026",
                        "11:00",
                        "38–40 BYN",
                        "https://www.ticketpro.by/bilety-v-teatr/" + index))
                .toList();

        // Act
        String result = MessageFormatter.formatTicketproEvents("ГУ Дворец Республики, Минск", events);

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
                        "• Спектакль 10\n12.09.2026, 11:00\n38–40 BYN\nhttps://www.ticketpro.by/bilety-v-teatr/10\n\n",
                        "• Спектакль 11\n12.09.2026, 11:00\n38–40 BYN\nhttps://www.ticketpro.by/bilety-v-teatr/11\n\n")
                .endsWith("И ещё новых событий: 2");
        assertThat(result.split("• ", -1).length - 1).isEqualTo(10);
    }

    @Test
    void shouldKeepShortTicketproEventAfterLongEventDoesNotFit() {
        // Arrange
        TicketproEvent longEvent = new TicketproEvent(
                "Длинный спектакль " + "Я".repeat(5000),
                "12.09.2026",
                "11:00",
                "38–40 BYN",
                "https://www.ticketpro.by/bilety-v-teatr/long");
        TicketproEvent shortEvent = new TicketproEvent(
                "Короткий спектакль",
                "12.09.2026",
                "11:00",
                "38–40 BYN",
                "https://www.ticketpro.by/bilety-v-teatr/short");

        // Act
        String result = MessageFormatter.formatTicketproEvents(
                "ГУ Дворец Республики, Минск", List.of(longEvent, shortEvent));

        // Assert
        assertThat(result.length()).isLessThanOrEqualTo(3800);
        assertThat(result)
                .contains(shortEvent.title())
                .doesNotContain(longEvent.title())
                .endsWith("И ещё новых событий: 1");
    }

    @Test
    void shouldFormatPuppetTheatreSession() {
        PuppetTheatreSession session = new PuppetTheatreSession(
                "Мойдодыр",
                LocalDate.of(2026, 10, 2),
                LocalTime.of(19, 30),
                "https://puppet-minsk.by/spektakli/mojdodyr/");

        String result = MessageFormatter.formatPuppetTheatreSessions(List.of(session));

        assertThat(result)
                .isEqualTo(
                        "🎭 В театре кукол появились новые сеансы\n\n"
                                + "• Мойдодыр\n"
                                + "02.10.2026, 19:30\n"
                                + "https://puppet-minsk.by/spektakli/mojdodyr/\n\n");
    }

    @Test
    void shouldLimitPuppetTheatreMessageToTenSessionBlocks() {
        List<PuppetTheatreSession> sessions = IntStream.range(0, 12)
                .mapToObj(index -> new PuppetTheatreSession(
                        "Спектакль " + index,
                        LocalDate.of(2026, 10, 2),
                        LocalTime.of(19, 30),
                        "https://puppet-minsk.by/spektakli/" + index))
                .toList();

        String result = MessageFormatter.formatPuppetTheatreSessions(sessions);

        assertThat(result.split("• ", -1).length - 1).isEqualTo(10);
        assertThat(result)
                .contains("• Спектакль 0\n", "• Спектакль 9\n")
                .doesNotContain("• Спектакль 10\n", "• Спектакль 11\n")
                .endsWith("И ещё новых сеансов: 2");
    }

    @Test
    void shouldKeepPuppetTheatreDescriptionWithinReservedLimit() {
        List<PuppetTheatreSession> sessions = IntStream.range(0, 10)
                .mapToObj(index -> new PuppetTheatreSession(
                        "Спектакль " + index + " " + "Я".repeat(700),
                        LocalDate.of(2026, 10, 2),
                        LocalTime.of(19, 30),
                        "https://puppet-minsk.by/spektakli/" + "x".repeat(300) + index))
                .toList();

        String result = MessageFormatter.formatPuppetTheatreSessions(sessions);

        assertThat(result.length()).isLessThanOrEqualTo(3800);
        assertThat(result).contains("И ещё новых сеансов:");
    }
}
