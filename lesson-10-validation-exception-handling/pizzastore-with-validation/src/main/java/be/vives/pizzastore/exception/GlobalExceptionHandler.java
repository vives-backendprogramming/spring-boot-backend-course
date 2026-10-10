package be.vives.pizzastore.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns every exception into an RFC 7807 {@link ProblemDetail} response.
 * <p>
 * Extending {@link ResponseEntityExceptionHandler} makes the exceptions Spring MVC itself throws
 * (405 Method Not Allowed, 404 No Resource Found, 400 type mismatch, 413 Payload Too Large, ...)
 * return a ProblemDetail as well. Those are customised by overriding its protected methods,
 * not with an extra {@code @ExceptionHandler} (that would be an ambiguous mapping).
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String ERROR_TYPE_BASE = "https://api.pizzastore.example.com/errors/";

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        log.warn("Validation failed for request: {}", request.getDescription(false));

        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> {
                    Map<String, String> entry = new LinkedHashMap<>();
                    entry.put("field", error.getField());
                    entry.put("message", error.getDefaultMessage());
                    return entry;
                })
                .toList();

        ProblemDetail problemDetail = buildProblemDetail(HttpStatus.BAD_REQUEST, "Validation failed", "validation", request);
        problemDetail.setProperty("errors", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).headers(headers).body(problemDetail);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        log.warn("Malformed JSON request: {}", ex.getMessage());

        String detail = "Malformed JSON request";
        if (ex.getCause() != null) {
            String causeMessage = ex.getCause().getMessage();
            if (causeMessage != null && causeMessage.contains("OrderStatus")) {
                detail = "Invalid OrderStatus value. Allowed values: PENDING, CONFIRMED, PREPARING, READY, DELIVERED, CANCELLED";
            }
        }

        ProblemDetail problemDetail = buildProblemDetail(HttpStatus.BAD_REQUEST, detail, "malformed-request", request);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).headers(headers).body(problemDetail);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        return buildProblemDetail(HttpStatus.NOT_FOUND, ex.getMessage(), "not-found", request);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ProblemDetail handleDuplicateResourceException(DuplicateResourceException ex, WebRequest request) {
        log.warn("Duplicate resource: {}", ex.getMessage());
        return buildProblemDetail(HttpStatus.CONFLICT, ex.getMessage(), "conflict", request);
    }

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusinessException(BusinessException ex, WebRequest request) {
        log.warn("Business logic error: {}", ex.getMessage());
        return buildProblemDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage(), "business-rule", request);
    }

    @ExceptionHandler(PizzaStoreException.class)
    public ProblemDetail handlePizzaStoreException(PizzaStoreException ex, WebRequest request) {
        log.warn("PizzaStore error: {}", ex.getMessage());
        return buildProblemDetail(HttpStatus.BAD_REQUEST, ex.getMessage(), "bad-request", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex, WebRequest request) {
        // Don't leak the SQL error to the client: log it, return a generic message
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return buildProblemDetail(HttpStatus.CONFLICT,
                "The request conflicts with existing data (e.g. the resource is still referenced by other data).",
                "conflict", request);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpectedException(Exception ex, WebRequest request) {
        log.error("Unexpected error occurred", ex);
        return buildProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please contact support if the problem persists.", "internal-error", request);
    }

    private ProblemDetail buildProblemDetail(HttpStatus status, String detail, String typeSlug, WebRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setType(URI.create(ERROR_TYPE_BASE + typeSlug));
        problemDetail.setInstance(URI.create(extractPath(request)));
        return problemDetail;
    }

    private String extractPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}
