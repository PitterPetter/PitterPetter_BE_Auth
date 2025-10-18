package PitterPatter.loventure.authService.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import PitterPatter.loventure.authService.dto.response.ApiResponse;
import PitterPatter.loventure.authService.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
@Slf4j
public class InternalApiController {

    private final AuthService authService;

    /**
     * Territory-service로부터 JWT 토큰 검증 요청을 받는 API
     * Territory-service가 GET 메서드로 호출
     */
    @GetMapping("/api/regions/verify")
    public ResponseEntity<?> verifyToken(
            @RequestHeader("Authorization") String authorization) {
        try {
            log.info("JWT 토큰 검증 요청 수신 - Authorization: {}", 
                    authorization != null ? "Bearer 토큰 존재" : "토큰 없음");
            
            // JWT 토큰 검증 로직
            if (authorization == null || !authorization.startsWith("Bearer ")) {
                log.warn("유효하지 않은 Authorization 헤더");
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("INVALID_TOKEN", "유효하지 않은 토큰입니다"));
            }
            
            String token = authorization.substring(7);
            
            // AuthService의 JWT 검증 메서드 사용
            boolean isValid = authService.verifyJwtToken(token);
            
            if (!isValid) {
                log.warn("JWT 토큰 검증 실패");
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("TOKEN_VERIFICATION_FAILED", "토큰 검증에 실패했습니다"));
            }
            
            log.info("JWT 토큰 검증 성공");
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "토큰 검증 성공");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("JWT 토큰 검증 실패 - error: {}", e.getMessage(), e);
            
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("TOKEN_VERIFICATION_ERROR", "토큰 검증 중 오류가 발생했습니다"));
        }
    }
}
