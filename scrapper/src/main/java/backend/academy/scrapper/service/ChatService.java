package backend.academy.scrapper.service;

import backend.academy.scrapper.repository.InMemoryRepository;
import org.springframework.stereotype.Service;

@Service
public class ChatService {
    private final InMemoryRepository inMemoryRepository;

    public ChatService(InMemoryRepository inMemoryRepository) {
        this.inMemoryRepository = inMemoryRepository;
    }

    public void registerChat(Long chatId) {
        inMemoryRepository.registerChat(chatId);
    }

    public void deleteChat(Long chatId) {
        inMemoryRepository.deleteChat(chatId);
    }
}
