package in.rbihub.lending.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.List;


public record ErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String path,

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        List<FieldError> fieldErrors
) {

    public record FieldError(String field, String message, Object rejectedValue) {
    }
}
