package com.openbas.intelligence.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Standardized API error response body for all error conditions.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorDto(
    String status,
    int code,
    String message,
    String path,
    Instant timestamp,
    List<FieldErrorDto> fieldErrors,
    String correlationId
) {
    public record FieldErrorDto(String field, String message, Object rejectedValue) {}

    public static ApiErrorDto of(int code, String message, String path, String correlationId) {
        return new ApiErrorDto(httpStatus(code), code, message, path, Instant.now(), null, correlationId);
    }

    public static ApiErrorDto withFieldErrors(int code, String message, String path,
                                               List<FieldErrorDto> fieldErrors, String correlationId) {
        return new ApiErrorDto(httpStatus(code), code, message, path, Instant.now(), fieldErrors, correlationId);
    }

    private static String httpStatus(int code) {
        return switch (code) {
            case 400 -> "BAD_REQUEST";
            case 401 -> "UNAUTHORIZED";
            case 403 -> "FORBIDDEN";
            case 404 -> "NOT_FOUND";
            case 409 -> "CONFLICT";
            case 422 -> "UNPROCESSABLE_ENTITY";
            case 429 -> "TOO_MANY_REQUESTS";
            case 500 -> "INTERNAL_SERVER_ERROR";
            case 503 -> "SERVICE_UNAVAILABLE";
            default -> "ERROR";
        };
    }
}
