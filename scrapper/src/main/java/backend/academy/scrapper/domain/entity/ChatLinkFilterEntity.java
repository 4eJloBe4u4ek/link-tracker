package backend.academy.scrapper.domain.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "chat_link_filters")
@Getter
@Setter
@NoArgsConstructor
public class ChatLinkFilterEntity {
    @EmbeddedId
    private ChatLinkFilterPK id;

    @ManyToOne
    @MapsId("chatId")
    private ChatEntity chat;

    @ManyToOne
    @MapsId("linkId")
    private LinkEntity link;

    @ManyToOne
    @MapsId("filterId")
    private FilterEntity filter;
}
