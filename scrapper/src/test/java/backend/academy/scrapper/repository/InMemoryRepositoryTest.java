package backend.academy.scrapper.repository;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import backend.academy.scrapper.exception.ChatAlreadyExistsException;
import backend.academy.scrapper.exception.ChatNotFoundException;
import backend.academy.scrapper.exception.LinkAlreadyExistsException;
import backend.academy.scrapper.exception.LinkNotFoundException;
import backend.academy.shared.dto.TrackedLink;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class InMemoryRepositoryTest {

    private InMemoryRepository repository;
    private final Long existingChatId = 123L;
    private final Long nonExistentChatId = 456L;
    private final String testUrl = "http://test.com";

    @BeforeEach
    void setUp() {
        repository = new InMemoryRepository();
        repository.registerChat(existingChatId);
    }

    @Test
    void registerNewChat_Success() {
        Long newChatId = 789L;
        repository.registerChat(newChatId);

        assertTrue(repository.trackedLinks().containsKey(newChatId));
        assertEquals(0, repository.getLinks(newChatId).size());
    }

    @Test
    void registerExistingChat_ThrowsException() {
        assertThrows(ChatAlreadyExistsException.class, () -> repository.registerChat(existingChatId));
    }

    @Test
    void deleteExistingChat_Success() {
        repository.deleteChat(existingChatId);

        assertFalse(repository.trackedLinks().containsKey(existingChatId));
    }

    @Test
    void deleteNonExistentChat_ThrowsException() {
        assertThrows(ChatNotFoundException.class, () -> repository.deleteChat(nonExistentChatId));
    }

    @Test
    void addLink_SavesDataCorrectly() {
        TrackedLink result = repository.addLink(existingChatId, testUrl, List.of("tag1"), List.of("filter1"));

        assertEquals(testUrl, result.url());
        assertEquals(List.of("tag1"), result.tags());
        assertEquals(List.of("filter1"), result.filters());
        assertNotNull(result.id());
        assertNotNull(result.createdAt());
    }

    @Test
    void addDuplicateLink_ThrowsException() {
        repository.addLink(existingChatId, testUrl, List.of(), List.of());

        assertThrows(
                LinkAlreadyExistsException.class,
                () -> repository.addLink(existingChatId, testUrl, List.of(), List.of()));
    }

    @Test
    void deleteLink_Success() {
        TrackedLink link = repository.addLink(existingChatId, testUrl, List.of(), List.of());

        TrackedLink removed = repository.removeLink(existingChatId, testUrl);

        assertEquals(link, removed);
        assertTrue(repository.getLinks(existingChatId).isEmpty());
    }

    @Test
    void deleteNonExistentLink_ThrowsException() {
        assertThrows(LinkNotFoundException.class, () -> repository.removeLink(existingChatId, testUrl));
    }
}
