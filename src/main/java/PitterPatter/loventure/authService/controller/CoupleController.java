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
import PitterPatter.loventure.authService.dto.request.CoupleOnboardingRequest;
import PitterPatter.loventure.authService.dto.response.ApiResponse;
import PitterPatter.loventure.authService.dto.response.CoupleMatchResponse;
import PitterPatter.loventure.authService.dto.response.CreateCoupleRoomResponse;
import PitterPatter.loventure.authService.repository.User;
import PitterPatter.loventure.authService.repository.UserRepository;
import PitterPatter.loventure.authService.service.CoupleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/couples")
@RequiredArgsConstructor
@Slf4j
public class CoupleController {
    
    private final CoupleService coupleService;
    private final UserRepository userRepository;

    @PostMapping("/rooms")
    // 이 API는 현재 인증된 OAuth2의 JWT에서 받아온 정보로 coupleRoom을 생성하는 API입니다.
    public ResponseEntity<ApiResponse<CreateCoupleRoomResponse>> createCoupleRoom(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        try {
            // JWT에서 providerId 추출
            String providerId = userDetails.getUsername();
            // prividerId를 이용해서 CoupleRoom 생성
            ApiResponse<CreateCoupleRoomResponse> response = coupleService.createCoupleRoom(providerId);
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
            // JWT에서 providerId와 userId 추출
            String providerId = userDetails.getUsername();
            // providerId로 사용자 조회 후 userId(TSID) 추출
            User user = userRepository.findByProviderId(providerId);
            if (user == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("40401", "사용자를 찾을 수 없습니다"));
            }
            String userId = user.getTsid().toString();
            request.setUserId(userId);
            
            ApiResponse<CoupleMatchResponse> response = coupleService.matchCouple(request);
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
    
    /**
     * 커플 온보딩 생성/수정
     */
    @PostMapping("/{coupleId}/onboarding")
    public ResponseEntity<ApiResponse<Void>> createOnboarding(
            @PathVariable String coupleId,
            @RequestBody @Valid CoupleOnboardingRequest request) {
        
        try {
            ApiResponse<Void> response = coupleService.createOrUpdateOnboarding(coupleId, request);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("커플 온보딩 API 오류: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("50001", "알 수 없는 서버 에러가 발생했습니다."));
        }
    }
    

}