package backend.academy.scrapper.scheduler.sender;

import backend.academy.shared.dto.LinkUpdate;
import reactor.core.publisher.Mono;

public interface UpdateSender {
    Mono<Void> sendUpdate(LinkUpdate linkUpdate);
}
