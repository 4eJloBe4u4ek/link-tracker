package backend.academy.scrapper.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class ChatLinkFilterPK {
    @Column(name = "chat_id")
    private Long chatId;

    @Column(name = "link_id")
    private Long linkId;

    @Column(name = "filter_id")
    private Long filterId;
}
