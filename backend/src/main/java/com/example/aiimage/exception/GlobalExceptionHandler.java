package com.example.aiimage.exception;

import com.example.aiimage.common.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        HttpStatus status = switch (ex.getCode()) {
            case 404 -> HttpStatus.NOT_FOUND;
            case 502 -> HttpStatus.BAD_GATEWAY;
            default -> ex.getCode() >= 500 ? HttpStatus.INTERNAL_SERVER_ERROR : HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(ApiResponse.failure(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage() == null ? "参数错误" : error.getDefaultMessage())
                .distinct()
                .collect(Collectors.joining("；"));
        return ResponseEntity.badRequest().body(ApiResponse.failure(400, message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.failure(400, ex.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.failure(400, "上传图片过大，单张图片不能超过 20MB"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknownException(Exception ex) {
        log.error("Unexpected server error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure(500, "服务器内部错误，请稍后重试"));
    }
}
