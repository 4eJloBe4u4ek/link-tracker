package backend.academy.scrapper;

import backend.academy.shared.dto.LinkUpdate;
import backend.academy.shared.dto.TrackedLink;
import java.time.LocalDateTime;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TestData {
    public static final Long TEST_CHAT_ID = 123L;
    public static final String TEST_URL = "https://example.com";
    public static final String GITHUB_URL = "https://github.com/owner/repo";
    public static final String STACKOVERFLOW_URL = "https://stackoverflow.com/questions";

    public static final String GITHUB_OWNER = "owner";
    public static final String GITHUB_REPO = "repo";
    public static final String GITHUB_COMMITS_PATH = "/repos/" + GITHUB_OWNER + "/" + GITHUB_REPO + "/commits";
    public static final String GITHUB_ISSUES_PATH = "/repos/" + GITHUB_OWNER + "/" + GITHUB_REPO + "/issues";
    public static final String GITHUB_COMMENTS_PATH = "/repos/" + GITHUB_OWNER + "/" + GITHUB_REPO + "/issues/comments";
    public static final String GITHUB_PULLS_PATH = "/repos/" + GITHUB_OWNER + "/" + GITHUB_REPO + "/pulls";

    public static final Long STACKOVERFLOW_QUESTION_ID = 123L;
    public static final Long STACKOVERFLOW_MIN = 123L;
    public static final Long STACKOVERFLOW_ANSWER_ID = 123L;
    public static final String STACKOVERFLOW_QUESTION_PATH = "/questions/" + STACKOVERFLOW_QUESTION_ID;
    public static final String STACKOVERFLOW_ANSWERS_PATH = STACKOVERFLOW_QUESTION_PATH + "/answers";
    public static final String STACKOVERFLOW_COMMENTS_TO_QUESTION_PATH = STACKOVERFLOW_QUESTION_PATH + "/comments";
    public static final String STACKOVERFLOW_COMMENTS_TO_ANSWER_PATH =
            "/answers/" + STACKOVERFLOW_QUESTION_ID + "/comments";

    public static final String TEST_TAG = "tag";
    public static final String TEST_FILTER = "filter";
    public static final String TEST_TIME = "2025-01-01T00:00:00";

    public static final String GITHUB_COMMITS_RESPONSE =
            """
                            [
                                {
                                    "commit": {
                                        "url": "https://api.github.com/repos/owner/repo/commits",
                                        "message": "Initial commit",
                                        "author": {
                                            "name": "Noname",
                                            "date": "2025-01-01T00:00:00Z"
                                        }
                                    }
                                }
                            ]
            """;
    public static final String GITHUB_COMMITS_RESPONSE_MESSAGE = "Initial commit";

    public static final String GITHUB_ISSUES_RESPONSE =
            """
                            [
                                {
                                    "id": 1,
                                    "title": "This is a issue",
                                    "body": "Issue body",
                                    "user": {
                                        "id": 1,
                                        "login": "TestUserLogin"
                                    },
                                    "state": "open",
                                    "created_at": "2025-01-01T00:00:00Z",
                                    "updated_at": "2025-01-01T00:00:00Z"
                                }
                            ]
            """;
    public static final String GITHUB_ISSUES_RESPONSE_TITLE = "This is a issue";

    public static final String GITHUB_COMMENTS_RESPONSE =
            """
                [
                    {
                        "id": 1,
                        "body": "This is a comment",
                        "user": {
                            "id": 1,
                            "login": "TestUserLogin"
                        },
                        "created_at": "2025-01-01T00:00:00Z",
                        "updated_at": "2025-01-01T00:00:00Z"
                    }
                ]
            """;
    public static final String GITHUB_COMMENTS_RESPONSE_BODY = "This is a comment";

    public static final String GITHUB_PULLS_RESPONSE =
            """
            [
                {
                    "id": 1,
                    "state": "open",
                    "title": "PR title",
                    "body": "This fixes an issue",
                    "user": {
                        "id": 1,
                        "login": "TestUserLogin"
                    },
                    "created_at": "2025-01-01T00:00:01Z",
                    "updated_at": "2025-01-01T00:00:00Z"
                }
            ]
            """;
    public static final String GITHUB_PULLS_RESPONSE_TITLE = "PR title";

    public static final String GITHUB_EMPTY_RESPONSE = "[]";

    public static final String STACKOVERFLOW_QUESTION_RESPONSE =
            """
            {
                "items": [
                    {
                        "question_id": 123,
                        "owner": {
                            "account_id": 123,
                            "display_name": "Noname",
                            "reputation": 123,
                            "user_id": 123
                        },
                        "creation_date": 123,
                        "last_activity_date": 123,
                        "last_edit_date": 123,
                        "title": "Test question title",
                        "is_answered": true,
                        "answer_count": 123
                    }
                ]
            }
            """;
    public static final String STACKOVERFLOW_QUESTION_RESPONSE_TITLE = "Test question title";

    public static final String STACKOVERFLOW_ANSWER_RESPONSE =
            """
            {
                "items": [
                    {
                        "answer_id": 1,
                        "question_id": 123,
                        "owner": {
                            "account_id": 123,
                            "display_name": "Noname",
                            "reputation": 123,
                            "user_id": 123
                        },
                        "body": "This is an answer",
                        "creation_date": 123,
                        "last_activity_date": 123,
                        "last_edit_date": 123
                    }
                ]
            }
            """;
    public static final String STACKOVERFLOW_ANSWER_RESPONSE_BODY = "This is an answer";

    public static final String STACKOVERFLOW_COMMENT_RESPONSE =
            """
            {
                "items": [
                    {
                        "comment_id": 1,
                        "owner": {
                            "account_id": 123,
                            "display_name": "Noname",
                            "reputation": 123,
                            "user_id": 123
                        },
                        "body": "This is a comment",
                        "creation_date": 123
                    }
                ]
            }
            """;
    public static final String STACKOVERFLOW_COMMENT_RESPONSE_BODY = "This is a comment";

    public static final String STACKOVERFLOW_EMPTY_RESPONSE = "{\"items\":[]}";

    public static final LinkUpdate LINK_UPDATE = new LinkUpdate(1L, TEST_URL, "description", List.of(TEST_CHAT_ID));

    public static final TrackedLink GITHUB_TRACKED_LINK = new TrackedLink(
            1L,
            GITHUB_URL,
            List.of(TEST_TAG),
            List.of(TEST_FILTER),
            LocalDateTime.parse(TEST_TIME),
            LocalDateTime.parse(TEST_TIME));

    public static final TrackedLink STACKOVERFLOW_TRACKED_LINK = new TrackedLink(
            1L,
            STACKOVERFLOW_URL + "/" + STACKOVERFLOW_QUESTION_ID,
            List.of(TEST_TAG),
            List.of(TEST_FILTER),
            LocalDateTime.parse(TEST_TIME),
            LocalDateTime.parse(TEST_TIME));

    public static final TrackedLink UNKNOWN_TRACKED_LINK = new TrackedLink(
            1L,
            TEST_URL,
            List.of(TEST_TAG),
            List.of(TEST_FILTER),
            LocalDateTime.parse(TEST_TIME),
            LocalDateTime.parse(TEST_TIME));
}
