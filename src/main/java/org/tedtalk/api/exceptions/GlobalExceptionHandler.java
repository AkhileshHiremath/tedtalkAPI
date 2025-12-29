package org.tedtalk.api.exceptions;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Global exception handler using Java 21 pattern matching features.
 *
 * This centralizes error handling across the entire application and uses modern
 * pattern matching to make exception handling more elegant.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MissingCsvColumnException.class)
    public ResponseEntity<ErrorResponse> handleMissingCsvColumn(MissingCsvColumnException ex,
                                                                ServerHttpRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Missing CSV column", ex.getMessage(), req.getURI().getPath());
    }

    @ExceptionHandler(CsvImportException.class)
    public ResponseEntity<ErrorResponse> handleCsvImport(CsvImportException ex,
                                                         ServerHttpRequest req) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "CSV import error", ex.getMessage(), req.getURI().getPath());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                          ServerHttpRequest req) {
        // Java 21: Using pattern matching with record patterns for cleaner code
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> switch (err.getField()) {
                    case String field when field.isEmpty() -> "Validation failed";
                    case String field -> field + ": " + err.getDefaultMessage();
                })
                .findFirst()
                .orElse("Validation failed");
        return build(HttpStatus.BAD_REQUEST, "Validation error", message, req.getURI().getPath());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, ServerHttpRequest req) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", ex.getMessage(), req.getURI().getPath());
    }

    /**
     * Build error response with proper HTTP status.
     * Java 21: Could use String templates here in future when fully stable.
     */
    private ResponseEntity<ErrorResponse> build(HttpStatus status, String error, String message, String path) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(status.value(), error, message, path));
    }
}
