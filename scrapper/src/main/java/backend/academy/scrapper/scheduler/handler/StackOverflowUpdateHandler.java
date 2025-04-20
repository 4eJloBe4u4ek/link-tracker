package backend.academy.scrapper.scheduler.handler;

import backend.academy.scrapper.client.stackoverflow.StackOverflowClient;
import backend.academy.scrapper.client.stackoverflow.StackOverflowQuestion;
import backend.academy.scrapper.scheduler.service.NotificationService;
import backend.academy.scrapper.scheduler.util.MessageFormatter;
import backend.academy.shared.dto.TrackedLink;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class StackOverflowUpdateHandler {
    private final NotificationService notificationService;
    private final StackOverflowClient stackOverflowClient;
    private static final Pattern STACKOVERFLOW_LINK_PATTERN =
            Pattern.compile("^https://stackoverflow\\.com/questions/(\\d+)/?.*");

    public static boolean isStackoverflowLink(String url) {
        return STACKOVERFLOW_LINK_PATTERN.matcher(url).matches();
    }

    public CompletableFuture<Void> handle(TrackedLink trackedLink) {
        Matcher matcher = STACKOVERFLOW_LINK_PATTERN.matcher(trackedLink.url());
        if (!matcher.matches()) {
            log.atWarn()
                    .setMessage("URL didn't match StackOverflow pattern")
                    .addKeyValue("url", trackedLink.url())
                    .log();
            return CompletableFuture.completedFuture(null);
        }
        Long questionId = Long.parseLong(matcher.group(1));

        return stackOverflowClient
                .getStackoverflowUpdates(questionId, trackedLink.updatedAt())
                .flatMap(stackoverflowUpdates -> {
                    if (stackoverflowUpdates == null) {
                        return Mono.empty();
                    }

                    StackOverflowQuestion stackoverflowQuestion = stackoverflowUpdates.question();

                    return Flux.concat(
                                    Flux.just(stackoverflowQuestion)
                                            .filter(question ->
                                                    question.lastActivityDate().isAfter(trackedLink.updatedAt()))
                                            .flatMap(question -> notificationService.sendUpdate(
                                                    trackedLink,
                                                    MessageFormatter.formatStackoverflowQuestion(question),
                                                    question.owner().displayName())),
                                    Flux.fromIterable(stackoverflowUpdates.answers())
                                            .flatMap(stackoverflowAnswer -> notificationService.sendUpdate(
                                                    trackedLink,
                                                    MessageFormatter.formatStackoverflowAnswer(
                                                            stackoverflowQuestion, stackoverflowAnswer),
                                                    stackoverflowAnswer.owner().displayName())),
                                    Flux.fromIterable(stackoverflowUpdates.commentsToQuestion())
                                            .flatMap(stackoverflowComment -> notificationService.sendUpdate(
                                                    trackedLink,
                                                    MessageFormatter.formatStackoverflowComment(
                                                            stackoverflowQuestion, stackoverflowComment),
                                                    stackoverflowComment.owner().displayName())),
                                    Flux.fromIterable(stackoverflowUpdates.commentsToAnswers())
                                            .flatMap(stackoverflowComment -> notificationService.sendUpdate(
                                                    trackedLink,
                                                    MessageFormatter.formatStackoverflowComment(
                                                            stackoverflowQuestion, stackoverflowComment),
                                                    stackoverflowComment.owner().displayName())))
                            .then();
                })
                .toFuture();
    }
}
