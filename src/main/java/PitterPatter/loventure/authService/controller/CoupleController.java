package PitterPatter.loventure.authService.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import PitterPatter.loventure.authService.dto.request.CoupleMatchRequest;
import PitterPatter.loventure.authService.dto.request.CreateCoupleRoomWithOnboardingRequest;
import PitterPatter.loventure.authService.dto.response.ApiResponse;
import PitterPatter.loventure.authService.dto.response.CoupleMatchResponse;
import PitterPatter.loventure.authService.dto.response.CreateCoupleRoomResponse;
import PitterPatter.loventure.authService.service.CoupleService;
import PitterPatter.loventure.authService.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/home/coupleroom")
@RequiredArgsConstructor
@Slf4j
public class CoupleController {
    
    private final CoupleService coupleService;
    private final UserService userService;

    @PostMapping
    // 커플룸 생성과 온보딩을 함께 처리하는 통합 API
    public ResponseEntity<ApiResponse<CreateCoupleRoomResponse>> createCoupleRoomWithOnboarding(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid CreateCoupleRoomWithOnboardingRequest request) {
        
        try {
            // JWT에서 providerId 추출
            String providerId = userService.extractProviderId(userDetails);
            // 커플룸 생성과 온보딩을 함께 처리
            ApiResponse<CreateCoupleRoomResponse> response = coupleService.createCoupleRoomWithOnboarding(providerId, request);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("커플룸 생성 API 오류: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("50001", "알 수 없는 서버 에러가 발생했습니다."));
        }
    }
    
    // 커플 매칭
    @PostMapping("/match")
    public ResponseEntity<ApiResponse<CoupleMatchResponse>> matchCouple(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid CoupleMatchRequest request) {
        
        try {
            // JWT에서 providerId 추출
            String providerId = userService.extractProviderId(userDetails);
            
            ApiResponse<CoupleMatchResponse> response = coupleService.matchCouple(providerId, request.inviteCode());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("커플 매칭 API 오류: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("50001", "알 수 없는 서버 에러가 발생했습니다."));
        }
    }
    
   // 이 API는 coupleId를 통해 커플 매칭을 취소하는 api입니다
    @DeleteMapping("/{coupleId}")
    public ResponseEntity<ApiResponse<Void>> cancelCouple(
            @PathVariable String coupleId) {
        
        try {
            ApiResponse<Void> response = coupleService.cancelCouple(coupleId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("커플 매칭 취소 API 오류: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("50001", "알 수 없는 서버 에러가 발생했습니다."));
        }
    }

}