package backend.academy.scrapper.exception;

public class UnsupportedLinkException extends RuntimeException {
    public UnsupportedLinkException(String message) {
        super(message);
    }
}
