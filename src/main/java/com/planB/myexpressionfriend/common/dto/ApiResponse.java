package com.planB.myexpressionfriend.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * API 공통 응답 형식
 * @param <T> 응답 데이터 타입
 */
@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private ErrorDetail error;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    // ============= 성공 응답 생성 메서드 =============

    /**
     * 성공 응답 (데이터 있음)
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .build();
    }

    /**
     * 성공 응답 (데이터 + 메시지)
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    /**
     * 성공 응답 (메시지만)
     */
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .build();
    }

    // ============= 실패 응답 생성 메서드 =============

    /**
     * 실패 응답 (메시지만)
     */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .build();
    }

    /**
     * 실패 응답 (메시지 + 에러 상세)
     */
    public static <T> ApiResponse<T> error(String message, String errorCode) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .error(ErrorDetail.builder()
                        .code(errorCode)
                        .build())
                .build();
    }

    /**
     * 실패 응답 (완전체)
     */
    public static <T> ApiResponse<T> error(String message, String errorCode, Object errorDetails) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .error(ErrorDetail.builder()
                        .code(errorCode)
                        .details(errorDetails)
                        .build())
                .build();
    }

    // ============= 에러 상세 정보 =============

    @Getter
    @Builder
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorDetail {
        private String code;
        private Object details;
    }
}
