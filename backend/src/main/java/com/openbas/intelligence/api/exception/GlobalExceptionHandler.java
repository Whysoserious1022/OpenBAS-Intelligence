package com.openbas.intelligence.api.exception;

import com.openbas.intelligence.api.dto.ApiErrorDto;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorDto> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        log.debug("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiErrorDto.of(404, ex.getMessage(), req.getRequestURI(), correlationId(req)));
    }

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<ApiErrorDto> handleConflict(ResourceAlreadyExistsException ex, HttpServletRequest req) {
        log.debug("Resource conflict: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiErrorDto.of(409, ex.getMessage(), req.getRequestURI(), correlationId(req)));
    }

    @ExceptionHandler(BusinessValidationException.class)
    public ResponseEntity<ApiErrorDto> handleBusinessValidation(BusinessValidationException ex, HttpServletRequest req) {
        log.warn("Business validation failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(ApiErrorDto.of(422, ex.getMessage(), req.getRequestURI(), correlationId(req)));
    }

    @ExceptionHandler(PolicyViolationException.class)
    public ResponseEntity<ApiErrorDto> handlePolicyViolation(PolicyViolationException ex, HttpServletRequest req) {
        log.warn("Policy violation: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiErrorDto.of(403, "Command rejected by policy engine: " + ex.getMessage(),
                req.getRequestURI(), correlationId(req)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorDto> handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<ApiErrorDto.FieldErrorDto> fieldErrors = ex.getBindingResult().getAllErrors().stream()
            .filter(e -> e instanceof FieldError)
            .map(e -> (FieldError) e)
            .map(fe -> new ApiErrorDto.FieldErrorDto(fe.getField(), fe.getDefaultMessage(), fe.getRejectedValue()))
            .collect(Collectors.toList());
        log.debug("Validation errors on request to {}: {} field(s)", req.getRequestURI(), fieldErrors.size());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiErrorDto.withFieldErrors(400, "Validation failed", req.getRequestURI(),
                fieldErrors, correlationId(req)));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorDto> handleAuthentication(AuthenticationException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiErrorDto.of(401, "Authentication required", req.getRequestURI(), correlationId(req)));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorDto> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiErrorDto.of(403, "Access denied", req.getRequestURI(), correlationId(req)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorDto> handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception on {} {}", req.getMethod(), req.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiErrorDto.of(500, "An internal error occurred. See application logs.",
                req.getRequestURI(), correlationId(req)));
    }

    private String correlationId(HttpServletRequest req) {
        String corr = req.getHeader("X-Correlation-Id");
        return (corr != null && !corr.isBlank()) ? corr : null;
    }
}
