package com.meetsmore.nittei.api.error;

import com.fasterxml.jackson.databind.JsonMappingException;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(NitteiApiException.class)
    public ResponseEntity<String> handleNitteiException(NitteiApiException ex) {
        return switch (ex.getCode()) {
            case BAD_CLIENT_DATA -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
            case UNPROCESSABLE_ENTITY -> ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(ex.getMessage());
            case CONFLICT -> ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
            case UNAUTHORIZED, UNIDENTIFIABLE_CLIENT -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
            case NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
            case INTERNAL_ERROR -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
        };
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<String> handleResponseStatusException(ResponseStatusException ex) {
        String message = ex.getReason() == null || ex.getReason().isBlank() ? ex.getStatusCode().toString() : ex.getReason();
        return ResponseEntity.status(ex.getStatusCode()).body(message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<String> handleNotReadable(HttpMessageNotReadableException ex) {
        Throwable root = rootCause(ex);
        String reason = root.getMessage() == null || root.getMessage().isBlank() ? "Invalid JSON body" : root.getMessage();
        String fieldPath = "";
        Throwable cause = ex.getCause();
        if (cause instanceof JsonMappingException mappingException && !mappingException.getPath().isEmpty()) {
            fieldPath = mappingException.getPath().stream()
                .map(ref -> ref.getFieldName() != null ? ref.getFieldName() : "[" + ref.getIndex() + "]")
                .collect(Collectors.joining("."));
        }
        String message = fieldPath.isBlank()
            ? "Failed to deserialize the JSON body into the target type: " + reason
            : "Failed to deserialize the JSON body into the target type: " + fieldPath + ": " + reason;
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(message);
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }
}
