package backend.academy.scrapper.scheduler.util;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import backend.academy.scrapper.client.github.GithubComment;
import backend.academy.scrapper.client.github.GithubCommit;
import backend.academy.scrapper.client.github.GithubIssue;
import backend.academy.scrapper.client.github.GithubPullRequest;
import backend.academy.scrapper.client.github.GithubUser;
import backend.academy.scrapper.client.stackoverflow.StackOverflowAnswer;
import backend.academy.scrapper.client.stackoverflow.StackOverflowComment;
import backend.academy.scrapper.client.stackoverflow.StackOverflowOwner;
import backend.academy.scrapper.client.stackoverflow.StackOverflowQuestion;
import java.time.LocalDateTime;
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
}
