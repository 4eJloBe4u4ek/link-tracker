package backend.academy.bot.exception;

import backend.academy.shared.dto.ApiErrorResponse;
import java.util.Arrays;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(LinkUpdateException.class)
    public ResponseEntity<ApiErrorResponse> handleLinkUpdateException(LinkUpdateException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse(
                        "Некорректные параметры запроса",
                        String.valueOf(HttpStatus.BAD_REQUEST),
                        ex.getClass().getSimpleName(),
                        ex.getMessage(),
                        Arrays.stream(ex.getStackTrace())
                                .map(StackTraceElement::toString)
                                .toList()));
    }
}
