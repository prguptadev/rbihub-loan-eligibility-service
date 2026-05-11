package in.rbihub.lending.api.error;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import in.rbihub.lending.api.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Translates the bouquet of "bad input" exceptions into a consistent
 * {@link ErrorResponse}. Anything else falls through to the generic 500
 * handler so we don't accidentally leak stack details to callers.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                           HttpServletRequest request) {
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .sorted(Comparator.comparing(FieldError::getField))
                .map(fe -> new ErrorResponse.FieldError(
                        fe.getField(),
                        fe.getDefaultMessage(),
                        fe.getRejectedValue()
                ))
                .collect(Collectors.toList());

        return badRequest(request, "Request validation failed", fieldErrors);
    }

    /**
     * Covers malformed JSON and unknown enum values. Jackson surfaces enum
     * misses as {@link InvalidFormatException} wrapped here - we dig out the
     * targeted field so the caller can fix the payload.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex,
                                                           HttpServletRequest request) {
        Throwable cause = ex.getMostSpecificCause();
        if (cause instanceof InvalidFormatException ife) {
            String field = pathOf(ife);
            String allowed = enumValuesIfApplicable(ife);
            String message = "Invalid value for '" + field + "'"
                    + (allowed.isEmpty() ? "" : " - allowed values are " + allowed);
            return badRequest(request, message, List.of(
                    new ErrorResponse.FieldError(field, message, ife.getValue())
            ));
        }
        return badRequest(request, "Malformed request body", List.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex,
                                                                HttpServletRequest request) {
        return badRequest(request, ex.getMessage(), List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex,
                                                          HttpServletRequest request) {
        // Log at error level - we shouldn't see these in steady state.
        log.error("Unhandled exception while serving {}", request.getRequestURI(), ex);

        ErrorResponse body = new ErrorResponse(
                OffsetDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "An unexpected error occurred",
                request.getRequestURI(),
                List.of()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private ResponseEntity<ErrorResponse> badRequest(HttpServletRequest request,
                                                      String message,
                                                      List<ErrorResponse.FieldError> fieldErrors) {
        ErrorResponse body = new ErrorResponse(
                OffsetDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                message,
                request.getRequestURI(),
                fieldErrors
        );
        return ResponseEntity.badRequest().body(body);
    }

    private static String pathOf(InvalidFormatException ife) {
        return StreamSupport.stream(ife.getPath().spliterator(), false)
                .map(JsonMappingException.Reference::getFieldName)
                .filter(name -> name != null && !name.isBlank())
                .collect(Collectors.joining("."));
    }

    private static String enumValuesIfApplicable(InvalidFormatException ife) {
        Class<?> target = ife.getTargetType();
        if (target == null || !target.isEnum()) {
            return "";
        }
        return Stream.of(target.getEnumConstants())
                .map(Object::toString)
                .collect(Collectors.joining(", ", "[", "]"));
    }
}
