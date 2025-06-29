package backend.academy.scrapper.exception;

public class FilterAlreadyExistsException extends RuntimeException {
    public FilterAlreadyExistsException(String message) {
        super(message);
    }
}
