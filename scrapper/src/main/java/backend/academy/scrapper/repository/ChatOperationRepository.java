package backend.academy.scrapper.repository;

public interface ChatOperationRepository {
    void registerChat(Long chatId);

    void deleteChat(Long chatId);
}
