package backend.academy.scrapper.exception;

public class FilterNotFoundException extends RuntimeException {
    public FilterNotFoundException(String message) {
        super(message);
    }
}
