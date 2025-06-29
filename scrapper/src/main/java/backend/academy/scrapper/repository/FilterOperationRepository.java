package backend.academy.scrapper.repository;

public interface FilterOperationRepository {
    void addFilterToLink(Long chatId, String url, String filter);

    void removeFilterFromLink(Long chatId, String url, String filter);
}
