package backend.academy.scrapper.scheduler;

import backend.academy.shared.dto.LinkUpdate;
import reactor.core.publisher.Mono;

public interface UpdateSender {
    Mono<Void> sendUpdate(LinkUpdate linkUpdate);
}
