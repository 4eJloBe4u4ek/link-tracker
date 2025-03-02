package backend.academy.shared.exception;

import backend.academy.shared.dto.ApiErrorResponse;

public class ApiException extends RuntimeException {
    public ApiException(ApiErrorResponse apiErrorResponse) {
        super(apiErrorResponse.exceptionMessage());
    }
}
