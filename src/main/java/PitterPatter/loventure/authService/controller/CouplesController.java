package PitterPatter.loventure.authService.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import PitterPatter.loventure.authService.dto.request.CoupleUpdateRequest;
import PitterPatter.loventure.authService.dto.response.ApiResponse;
import PitterPatter.loventure.authService.service.CoupleService;
import PitterPatter.loventure.authService.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/couples")
@RequiredArgsConstructor
@Slf4j
public class CouplesController {
    
    private final CoupleService coupleService;
    private final UserService userService;

    // 커플 정보 변경 (경로 변수 없음)
    @PutMapping
    public ResponseEntity<ApiResponse<Void>> updateCoupleInfo(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid CoupleUpdateRequest request) {
        
        try {
            // JWT에서 providerId 추출
            String providerId = userService.extractProviderId(userDetails);
            
            ApiResponse<Void> response = coupleService.updateCoupleInfo(providerId, request);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("커플 정보 변경 API 오류: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("50001", "알 수 없는 서버 에러가 발생했습니다."));
        }
    }

    // 커플 정보 변경 (경로 변수 포함)
    @PutMapping("/{coupleId}")
    public ResponseEntity<ApiResponse<Void>> updateCoupleInfoById(
            @PathVariable String coupleId,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid CoupleUpdateRequest request) {
        
        try {
            // JWT에서 providerId 추출
            String providerId = userService.extractProviderId(userDetails);
            
            ApiResponse<Void> response = coupleService.updateCoupleInfo(providerId, request);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("커플 정보 변경 API 오류 (coupleId: {}): {}", coupleId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("50001", "알 수 없는 서버 에러가 발생했습니다."));
        }
    }
}


