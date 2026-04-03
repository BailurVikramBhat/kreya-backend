package com.kreya.shared.dto;

import java.time.Instant;

public record ApiResponse<T>(
    boolean success,
    T data,
    String message,
    Instant timestamp,
    String path,
    Object errors
) {

    public static <T> ApiResponse<T> success(T data, String message, String path) {
        return new ApiResponse<>(true, data, message, Instant.now(), path, null);
    }
}
