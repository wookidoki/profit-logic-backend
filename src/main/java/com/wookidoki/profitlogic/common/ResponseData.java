package com.wookidoki.profitlogic.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseData<T> {

    private final boolean success;
    private final T data;
    private final String message;

    @Builder.Default
    private final LocalDateTime timestamp = LocalDateTime.now();

    public static <T> ResponseData<T> success(T data) {
        return ResponseData.<T>builder()
                .success(true)
                .data(data)
                .build();
    }

    public static ResponseData<Void> success() {
        return ResponseData.<Void>builder()
                .success(true)
                .build();
    }

    public static <T> ResponseData<T> success(T data, String message) {
        return ResponseData.<T>builder()
                .success(true)
                .data(data)
                .message(message)
                .build();
    }

    public static <T> ResponseData<T> fail(String message) {
        return ResponseData.<T>builder()
                .success(false)
                .message(message)
                .build();
    }
}
