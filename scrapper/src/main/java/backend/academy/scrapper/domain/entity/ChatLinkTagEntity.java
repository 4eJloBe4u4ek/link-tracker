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
@Table(name = "chat_link_tags")
@Getter
@Setter
@NoArgsConstructor
public class ChatLinkTagEntity {
    @EmbeddedId
    private ChatLinkTagPK id;

    @ManyToOne
    @MapsId("chatId")
    private ChatEntity chat;

    @ManyToOne
    @MapsId("linkId")
    private LinkEntity link;

    @ManyToOne
    @MapsId("tagId")
    private TagEntity tag;
}
