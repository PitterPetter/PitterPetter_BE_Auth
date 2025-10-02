package PitterPatter.loventure.authService.util;

import org.springframework.http.ResponseEntity;

import PitterPatter.loventure.authService.dto.response.ApiResponse;
import PitterPatter.loventure.authService.exception.ErrorCode;

public class ResponseUtil {
    
    public static <T> ResponseEntity<ApiResponse<T>> success(T data) {
        return ResponseEntity.ok(ApiResponse.success(data));
    }
    
    public static <T> ResponseEntity<ApiResponse<T>> success(T data, String message) {
        return ResponseEntity.ok(ApiResponse.success(data, message));
    }
    
    public static <T> ResponseEntity<ApiResponse<T>> badRequest(ErrorCode errorCode) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(errorCode.getCode(), errorCode.getMessage()));
    }
    
    public static <T> ResponseEntity<ApiResponse<T>> badRequest(ErrorCode errorCode, String customMessage) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(errorCode.getCode(), customMessage));
    }
    
    public static <T> ResponseEntity<ApiResponse<T>> notFound(ErrorCode errorCode) {
        return ResponseEntity.status(404)
                .body(ApiResponse.error(errorCode.getCode(), errorCode.getMessage()));
    }
    
    public static <T> ResponseEntity<ApiResponse<T>> unauthorized(ErrorCode errorCode) {
        return ResponseEntity.status(401)
                .body(ApiResponse.error(errorCode.getCode(), errorCode.getMessage()));
    }
    
    public static <T> ResponseEntity<ApiResponse<T>> conflict(ErrorCode errorCode) {
        return ResponseEntity.status(409)
                .body(ApiResponse.error(errorCode.getCode(), errorCode.getMessage()));
    }
    
    public static <T> ResponseEntity<ApiResponse<T>> internalServerError(ErrorCode errorCode, String customMessage) {
        return ResponseEntity.internalServerError()
                .body(ApiResponse.error(errorCode.getCode(), customMessage));
    }
}
