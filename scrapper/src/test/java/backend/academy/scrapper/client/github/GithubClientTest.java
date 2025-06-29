package backend.academy.scrapper.client.github;

import static backend.academy.scrapper.TestData.GITHUB_COMMENTS_PATH;
import static backend.academy.scrapper.TestData.GITHUB_COMMENTS_RESPONSE;
import static backend.academy.scrapper.TestData.GITHUB_COMMENTS_RESPONSE_BODY;
import static backend.academy.scrapper.TestData.GITHUB_COMMITS_PATH;
import static backend.academy.scrapper.TestData.GITHUB_COMMITS_RESPONSE;
import static backend.academy.scrapper.TestData.GITHUB_COMMITS_RESPONSE_MESSAGE;
import static backend.academy.scrapper.TestData.GITHUB_EMPTY_RESPONSE;
import static backend.academy.scrapper.TestData.GITHUB_ISSUES_PATH;
import static backend.academy.scrapper.TestData.GITHUB_ISSUES_RESPONSE;
import static backend.academy.scrapper.TestData.GITHUB_ISSUES_RESPONSE_TITLE;
import static backend.academy.scrapper.TestData.GITHUB_OWNER;
import static backend.academy.scrapper.TestData.GITHUB_PULLS_PATH;
import static backend.academy.scrapper.TestData.GITHUB_PULLS_RESPONSE;
import static backend.academy.scrapper.TestData.GITHUB_PULLS_RESPONSE_TITLE;
import static backend.academy.scrapper.TestData.GITHUB_REPO;
import static backend.academy.scrapper.TestData.TEST_TIME;
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
        wireMock.stubFor(get(urlPathEqualTo(GITHUB_COMMITS_PATH))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(GITHUB_COMMITS_RESPONSE)));

        StepVerifier.create(githubClient.getCommits(GITHUB_OWNER, GITHUB_REPO, TEST_TIME))
                .expectNextMatches(commits -> commits.size() == 1
                        && commits.getFirst().commit().message().equals(GITHUB_COMMITS_RESPONSE_MESSAGE))
                .verifyComplete();
    }

    @Test
    void shouldReturnIssues() {
        wireMock.stubFor(get(urlPathEqualTo(GITHUB_ISSUES_PATH))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(GITHUB_ISSUES_RESPONSE)));

        StepVerifier.create(githubClient.getIssues(GITHUB_OWNER, GITHUB_REPO, TEST_TIME))
                .expectNextMatches(issues ->
                        issues.size() == 1 && issues.getFirst().title().equals(GITHUB_ISSUES_RESPONSE_TITLE))
                .verifyComplete();
    }

    @Test
    void shouldReturnComments() {
        wireMock.stubFor(get(urlPathEqualTo(GITHUB_COMMENTS_PATH))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(GITHUB_COMMENTS_RESPONSE)));

        StepVerifier.create(githubClient.getComments(GITHUB_OWNER, GITHUB_REPO, TEST_TIME))
                .expectNextMatches(comments ->
                        comments.size() == 1 && comments.getFirst().body().equals(GITHUB_COMMENTS_RESPONSE_BODY))
                .verifyComplete();
    }

    @Test
    void shouldReturnPullRequests() {
        wireMock.stubFor(get(urlPathEqualTo(GITHUB_PULLS_PATH))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(GITHUB_PULLS_RESPONSE)));

        StepVerifier.create(githubClient.getPullRequests(GITHUB_OWNER, GITHUB_REPO, LocalDateTime.parse(TEST_TIME)))
                .expectNextMatches(
                        prs -> prs.size() == 1 && prs.getFirst().title().equals(GITHUB_PULLS_RESPONSE_TITLE))
                .verifyComplete();
    }

    @Test
    void shouldHandleEmptyResponse() {
        wireMock.stubFor(get(urlPathEqualTo(GITHUB_COMMITS_PATH))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(GITHUB_EMPTY_RESPONSE)));

        StepVerifier.create(githubClient.getCommits(GITHUB_OWNER, GITHUB_REPO, TEST_TIME))
                .expectNextMatches(List::isEmpty)
                .verifyComplete();
    }
}
