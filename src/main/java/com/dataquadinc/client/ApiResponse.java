package com.dataquadinc.client;

import lombok.*;
import org.springframework.http.HttpStatus;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ApiResponse<T> {

    private boolean success;
    private T data;
    private String message;
    private int statusCode;

    public static <T> ApiResponse<T> success(T data, String message, HttpStatus status){
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .statusCode(status.value())
                .build();
    }
    public static <T> ApiResponse<T> error(String message,HttpStatus status){
        return ApiResponse.<T>builder()
                .success(false)
                .data(null)
                .statusCode(status.value())
                .message(message)
                .build();
    }
}
