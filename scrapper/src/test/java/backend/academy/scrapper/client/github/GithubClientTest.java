package backend.academy.scrapper.client.github;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import backend.academy.scrapper.config.ScrapperConfig;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import reactor.test.StepVerifier;

@ExtendWith(WireMockExtension.class)
public class GithubClientTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private GithubClient githubClient;

    @BeforeEach
    void setUp() {
        ScrapperConfig.GithubCredentials githubCredentials =
                new ScrapperConfig.GithubCredentials("token", wireMock.baseUrl());
        ScrapperConfig config = new ScrapperConfig(null, null, 100, null, githubCredentials, null, null);

        githubClient = new GithubClient(config);
    }

    @Test
    void shouldReturnCommits() {
        wireMock.stubFor(
                get(urlPathEqualTo("/repos/owner/repo/commits"))
                        .willReturn(
                                aResponse()
                                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                        .withBody(
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
                    """)));

        StepVerifier.create(githubClient.getCommits("owner", "repo", "2025-01-01T00:00:00Z"))
                .expectNextMatches(commits -> commits.size() == 1
                        && commits.getFirst().commit().message().equals("Initial commit"))
                .verifyComplete();
    }

    @Test
    void shouldReturnIssues() {
        wireMock.stubFor(
                get(urlPathEqualTo("/repos/owner/repo/issues"))
                        .willReturn(
                                aResponse()
                                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                        .withBody(
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
                    """)));

        StepVerifier.create(githubClient.getIssues("owner", "repo", "2025-01-01T00:00:00Z"))
                .expectNextMatches(issues ->
                        issues.size() == 1 && issues.getFirst().title().equals("This is a issue"))
                .verifyComplete();
    }

    @Test
    void shouldReturnComments() {
        wireMock.stubFor(
                get(urlPathEqualTo("/repos/owner/repo/issues/comments"))
                        .willReturn(
                                aResponse()
                                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                        .withBody(
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
                    """)));

        StepVerifier.create(githubClient.getComments("owner", "repo", "2025-01-01T00:00:00Z"))
                .expectNextMatches(comments ->
                        comments.size() == 1 && comments.getFirst().body().equals("This is a comment"))
                .verifyComplete();
    }

    @Test
    void shouldReturnPullRequests() {
        wireMock.stubFor(
                get(urlPathEqualTo("/repos/owner/repo/pulls"))
                        .willReturn(
                                aResponse()
                                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                        .withBody(
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
                        """)));

        StepVerifier.create(githubClient.getPullRequests("owner", "repo", LocalDateTime.parse("2025-01-01T00:00:00")))
                .expectNextMatches(
                        prs -> prs.size() == 1 && prs.getFirst().title().equals("PR title"))
                .verifyComplete();
    }

    @Test
    void shouldHandleEmptyResponse() {
        wireMock.stubFor(get(urlPathEqualTo("/repos/owner/repo/commits"))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("[]")));

        StepVerifier.create(githubClient.getCommits("owner", "repo", "2025-01-01T00:00:00Z"))
                .expectNextMatches(List::isEmpty)
                .verifyComplete();
    }
}
