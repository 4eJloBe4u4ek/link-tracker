package backend.academy.scrapper.repository;

public interface TagOperationRepository {
    void addTagToLink(Long chatId, String url, String tag);

    void removeTagFromLink(Long chatId, String url, String tag);
}
