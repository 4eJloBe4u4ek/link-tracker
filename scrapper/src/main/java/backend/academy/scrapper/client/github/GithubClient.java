package backend.academy.scrapper.client.github;

import backend.academy.scrapper.config.ScrapperConfig;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class GithubClient {
    private final WebClient githubClient;

    public GithubClient(ScrapperConfig scrapperConfig) {
        this.githubClient = WebClient.builder()
                .baseUrl(scrapperConfig.github().baseUrl())
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .defaultHeader(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + scrapperConfig.github().token())
                .build();
    }

    public Mono<GithubRepositoryUpdates> getRepositoryUpdates(String owner, String repo, LocalDateTime lastUpdatedAt) {
        // todo подумать над временем
        Instant instant = lastUpdatedAt.atZone(ZoneId.systemDefault()).toInstant();
        String since = DateTimeFormatter.ISO_INSTANT.format(instant);

        Mono<List<GithubCommit>> commitMono = getCommits(owner, repo, since);
        Mono<List<GithubIssue>> issuesMono = getIssues(owner, repo, since);
        Mono<List<GithubComment>> commentMono = getComments(owner, repo, since);
        Mono<List<GithubPullRequest>> pullRequestsMono = getPullRequests(owner, repo, lastUpdatedAt);

        return Mono.zip(commitMono, issuesMono, commentMono, pullRequestsMono)
                .map(tuple -> new GithubRepositoryUpdates(tuple.getT1(), tuple.getT2(), tuple.getT3(), tuple.getT4()));
    }

    public Mono<List<GithubCommit>> getCommits(String owner, String repo, String since) {
        return githubClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/repos/{owner}/{repo}/commits")
                        .queryParam("since", since)
                        .build(owner, repo))
                .retrieve()
                .bodyToFlux(GithubCommit.class)
                .collectList();
    }

    public Mono<List<GithubIssue>> getIssues(String owner, String repo, String since) {
        return githubClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/repos/{owner}/{repo}/issues")
                        .queryParam("state", "all")
                        .queryParam("since", since)
                        .build(owner, repo))
                .retrieve()
                .bodyToFlux(GithubIssue.class)
                .collectList();
    }

    public Mono<List<GithubComment>> getComments(String owner, String repo, String since) {
        return githubClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/repos/{owner}/{repo}/issues/comments")
                        .queryParam("since", since)
                        .build(owner, repo))
                .retrieve()
                .bodyToFlux(GithubComment.class)
                .collectList();
    }

    public Mono<List<GithubPullRequest>> getPullRequests(String owner, String repo, LocalDateTime lastUpdatedAt) {
        return githubClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/repos/{owner}/{repo}/pulls")
                        .queryParam("state", "all")
                        .build(owner, repo))
                .retrieve()
                .bodyToFlux(GithubPullRequest.class)
                .filter(pr -> pr.createdAt().isAfter(lastUpdatedAt))
                .collectList();
    }
}
