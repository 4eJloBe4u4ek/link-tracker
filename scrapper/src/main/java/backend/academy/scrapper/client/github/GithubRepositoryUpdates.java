package backend.academy.scrapper.client.github;

import java.util.List;

public record GithubRepositoryUpdates(
        List<GithubCommit> commits, List<GithubIssue> issues, List<GithubComment> comments) {}
