package backend.academy.scrapper.client.github;

import backend.academy.scrapper.config.ScrapperConfig;
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
                .baseUrl(scrapperConfig.githubBaseUrl())
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + scrapperConfig.githubToken())
                .build();
    }

    public Mono<GithubRepositoryUpdates> getRepositoryUpdates(String owner, String repo) {
        Mono<List<GithubCommit>> commitMono = getCommits(owner, repo);
        Mono<List<GithubIssue>> issuesMono = getIssues(owner, repo);
        Mono<List<GithubComment>> commentMono = getComments(owner, repo);

        return Mono.zip(commitMono, issuesMono, commentMono)
                .map(tuple -> new GithubRepositoryUpdates(tuple.getT1(), tuple.getT2(), tuple.getT3()));
    }

    public Mono<List<GithubCommit>> getCommits(String owner, String repo) {
        return githubClient
                .get()
                .uri("/repos/{owner}/{repo}/commits", owner, repo)
                .retrieve()
                .bodyToFlux(GithubCommit.class)
                .collectList();
    }

    public Mono<List<GithubIssue>> getIssues(String owner, String repo) {
        return githubClient
                .get()
                .uri("/repos/{owner}/{repo}/issues", owner, repo)
                .retrieve()
                .bodyToFlux(GithubIssue.class)
                .collectList();
    }

    public Mono<List<GithubComment>> getComments(String owner, String repo) {
        return githubClient
                .get()
                .uri("/repos/{owner}/{repo}/issues/comments", owner, repo)
                .retrieve()
                .bodyToFlux(GithubComment.class)
                .collectList();
    }
}
