package com.meetsmore.nittei.api.error;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.meetsmore.nittei.api.config.HttpLoggingFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler(NitteiApiException.class)
  public ResponseEntity<ApiErrorResponse> handleNitteiException(
      NitteiApiException ex, HttpServletRequest request) {
    return switch (ex.getCode()) {
      case BAD_CLIENT_DATA -> error(HttpStatus.BAD_REQUEST, ex.getCode(), ex.getMessage(), request);
      case UNPROCESSABLE_ENTITY ->
          error(HttpStatus.UNPROCESSABLE_ENTITY, ex.getCode(), ex.getMessage(), request);
      case CONFLICT -> error(HttpStatus.CONFLICT, ex.getCode(), ex.getMessage(), request);
      case UNAUTHORIZED, UNIDENTIFIABLE_CLIENT ->
          error(HttpStatus.UNAUTHORIZED, ex.getCode(), ex.getMessage(), request);
      case NOT_FOUND -> error(HttpStatus.NOT_FOUND, ex.getCode(), ex.getMessage(), request);
      case INTERNAL_ERROR ->
          error(HttpStatus.INTERNAL_SERVER_ERROR, ex.getCode(), "Internal server error", request);
    };
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ApiErrorResponse> handleResponseStatusException(
      ResponseStatusException ex, HttpServletRequest request) {
    String reason = ex.getReason();
    String message = reason == null || reason.isBlank() ? ex.getStatusCode().toString() : reason;
    return error(
        HttpStatus.valueOf(ex.getStatusCode().value()),
        NitteiErrorCode.BAD_CLIENT_DATA,
        message,
        request);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiErrorResponse> handleNotReadable(
      HttpMessageNotReadableException ex, HttpServletRequest request) {
    Throwable root = rootCause(ex);
    String reason =
        root.getMessage() == null || root.getMessage().isBlank()
            ? "Invalid JSON body"
            : root.getMessage();
    String fieldPath = "";
    Throwable cause = ex.getCause();
    if (cause instanceof JsonMappingException mappingException
        && !mappingException.getPath().isEmpty()) {
      fieldPath =
          mappingException.getPath().stream()
              .map(
                  ref ->
                      ref.getFieldName() != null ? ref.getFieldName() : "[" + ref.getIndex() + "]")
              .collect(Collectors.joining("."));
    }
    String message =
        fieldPath.isBlank()
            ? "Failed to deserialize the JSON body into the target type: " + reason
            : "Failed to deserialize the JSON body into the target type: "
                + fieldPath
                + ": "
                + reason;
    return error(
        HttpStatus.UNPROCESSABLE_ENTITY, NitteiErrorCode.UNPROCESSABLE_ENTITY, message, request);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    String details =
        ex.getBindingResult().getFieldErrors().stream()
            .map(this::formatFieldError)
            .collect(Collectors.joining(", "));
    String message = details.isBlank() ? "Validation failed" : "Validation failed: " + details;
    return error(
        HttpStatus.UNPROCESSABLE_ENTITY, NitteiErrorCode.UNPROCESSABLE_ENTITY, message, request);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
      ConstraintViolationException ex, HttpServletRequest request) {
    String details =
        ex.getConstraintViolations().stream()
            .map(v -> v.getPropertyPath() + ": " + v.getMessage())
            .collect(Collectors.joining(", "));
    String message = details.isBlank() ? "Validation failed" : "Validation failed: " + details;
    return error(
        HttpStatus.UNPROCESSABLE_ENTITY, NitteiErrorCode.UNPROCESSABLE_ENTITY, message, request);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleUnexpected(
      Exception ex, HttpServletRequest request) {
    log.error("Unhandled exception for {} {}", request.getMethod(), request.getRequestURI(), ex);
    return error(
        HttpStatus.INTERNAL_SERVER_ERROR,
        NitteiErrorCode.INTERNAL_ERROR,
        "Internal server error",
        request);
  }

  private String formatFieldError(FieldError error) {
    String field =
        error.getField() == null || error.getField().isBlank() ? "field" : error.getField();
    String message =
        error.getDefaultMessage() == null || error.getDefaultMessage().isBlank()
            ? "is invalid"
            : error.getDefaultMessage();
    return field + ": " + message;
  }

  private Throwable rootCause(Throwable throwable) {
    Throwable current = throwable;
    while (current.getCause() != null) {
      current = current.getCause();
    }
    return current;
  }

  private ResponseEntity<ApiErrorResponse> error(
      HttpStatus status, NitteiErrorCode code, String message, HttpServletRequest request) {
    return ResponseEntity.status(status)
        .body(
            ApiErrorResponse.of(
                status.value(), code.name(), message, request.getRequestURI(), requestId(request)));
  }

  private String requestId(HttpServletRequest request) {
    Object requestId = request.getAttribute(HttpLoggingFilter.REQUEST_ID_MDC_KEY);
    return requestId == null ? null : String.valueOf(requestId);
  }
}
