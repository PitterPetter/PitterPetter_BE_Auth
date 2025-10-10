package PitterPatter.loventure.authService.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import PitterPatter.loventure.authService.dto.UserDto;
import PitterPatter.loventure.authService.dto.request.OnboardingRequest;
import PitterPatter.loventure.authService.dto.response.ApiResponse;
import PitterPatter.loventure.authService.dto.response.OnboardingResponse;
import PitterPatter.loventure.authService.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
@Slf4j
public class OnboardingController {

    private final UserService userService;

    @PostMapping("/me")
    public ResponseEntity<ApiResponse<OnboardingResponse>> updateOnboardingInfo(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid OnboardingRequest onboardingRequest) {

        try {
            // 개발용: 인증이 우회된 경우 테스트용 providerId 사용
            String providerId;
            if (userDetails == null) {
                log.warn("인증이 우회된 상태에서 온보딩 API 호출 - 테스트용 providerId 사용");
                providerId = "test_provider_id"; // 테스트용 providerId
            } else {
                providerId = userService.extractProviderId(userDetails);
            }
            
            UserDto updatedUser = userService.updateOnboardingInfo(providerId, onboardingRequest);
            
            OnboardingResponse response = new OnboardingResponse(
                    updatedUser.userId().toString(),
                    true
            );

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            log.error("온보딩 정보 업데이트 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("50001", "알 수 없는 서버 에러가 발생했습니다.(" + e.getMessage() + ")"));
        }
    }

}
