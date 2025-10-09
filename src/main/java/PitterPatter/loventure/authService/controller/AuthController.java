package PitterPatter.loventure.authService.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import PitterPatter.loventure.authService.constants.RedirectStatus;
import PitterPatter.loventure.authService.dto.request.SignupRequest;
import PitterPatter.loventure.authService.dto.response.ApiResponse;
import PitterPatter.loventure.authService.dto.response.AuthResponse;
import PitterPatter.loventure.authService.dto.response.ErrorResponse;
import PitterPatter.loventure.authService.dto.response.MyPageResponse;
import PitterPatter.loventure.authService.dto.response.SignupResponse;
import PitterPatter.loventure.authService.exception.BusinessException;
import PitterPatter.loventure.authService.mapper.MyPageMapper;
import PitterPatter.loventure.authService.repository.CoupleRoom;
import PitterPatter.loventure.authService.repository.User;
import PitterPatter.loventure.authService.service.AuthService;
import PitterPatter.loventure.authService.service.CoupleService;
import PitterPatter.loventure.authService.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final CoupleService coupleService;
    private final MyPageMapper myPageMapper;

    @Value("${spring.jwt.redirect.onboarding}")
    private String onboardingRedirectUrl;

    @Value("${spring.jwt.redirect.coupleroom}")
    private String coupleroomRedirectUrl;

    @Value("${spring.jwt.redirect.home}")
    private String homeRedirectUrl;
    /**
     * 토큰 갱신 (쿠키에서 refresh token 읽기)
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        try {
            // 쿠키에서 refresh token 추출
            String refreshToken = authService.getRefreshTokenFromCookie(request);
            if (refreshToken == null) {
                return ResponseEntity.badRequest()
                        .body(ErrorResponse.of("리프레시 토큰이 없습니다", "REFRESH_TOKEN_NOT_FOUND"));
            }

            AuthResponse authResponse = authService.refreshToken(refreshToken);
            if (authResponse.success()) {
                // 새로운 refresh token을 쿠키에 저장
                authService.setRefreshTokenCookie(response, authResponse.refreshToken());
                return ResponseEntity.ok(authResponse);
            } else {
                return ResponseEntity.badRequest().body(authResponse);
            }
        } catch (Exception e) {
            log.error("토큰 갱신 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ErrorResponse.of("토큰 갱신 중 오류가 발생했습니다", "REFRESH_TOKEN_ERROR"));
        }
    }


    /**
     * 사용자 프로필 상세 조회 (마이페이지용)
     * @AuthenticationPrincipal을 사용하여 JWTFilter가 검증하고 SecurityContext에 저장한
     * 인증된 사용자 정보를 직접 주입받습니다.
     */
    @GetMapping("/mypage")
    public ResponseEntity<?> getMyPage(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            if (userDetails == null) {
                return ResponseEntity.badRequest()
                        .body(ErrorResponse.of("인증된 사용자 정보를 찾을 수 없습니다", "UNAUTHORIZED"));
            }

            String providerId = userDetails.getUsername();
            User user = userService.validateUserByProviderId(providerId);

            // 커플 정보 조회
            Optional<CoupleRoom> coupleRoomOpt = coupleService.getCoupleInfo(providerId);
            User partner = null;
            
            if (coupleRoomOpt.isPresent()) {
                CoupleRoom coupleRoom = coupleRoomOpt.get();
                // 파트너 정보 조회 (파트너가 존재하는 경우에만)
                String partnerProviderId = coupleRoom.getCreatorUserId().equals(providerId) 
                    ? coupleRoom.getPartnerUserId() 
                    : coupleRoom.getCreatorUserId();
                
                if (partnerProviderId != null) {
                    try {
                        partner = userService.validateUserByProviderId(partnerProviderId);
                    } catch (BusinessException e) {
                        log.warn("파트너 사용자 정보를 찾을 수 없습니다: {}", partnerProviderId);
                        // 파트너 정보가 없어도 커플룸 정보는 표시
                    }
                }
            }

            // 매퍼를 사용하여 MyPageResponse 생성
            MyPageResponse myPageResponse = myPageMapper.toMyPageResponse(user, coupleRoomOpt, partner);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", myPageResponse);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("마이페이지 정보 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ErrorResponse.of("마이페이지 정보 조회 중 오류가 발생했습니다", "MYPAGE_ERROR"));
        }
    }

    /**
     * 사용자 프로필 수정
     */
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@AuthenticationPrincipal UserDetails userDetails,
                                         @RequestBody @Valid PitterPatter.loventure.authService.dto.request.ProfileUpdateRequest request) {
        try {
            if (userDetails == null) {
                return ResponseEntity.badRequest()
                        .body(ErrorResponse.of("인증된 사용자 정보를 찾을 수 없습니다", "UNAUTHORIZED"));
            }

            String providerId = userDetails.getUsername();
            User updatedUser = userService.updateProfile(providerId, request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "프로필이 성공적으로 수정되었습니다");
            response.put("userId", updatedUser.getUserId().toString());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("프로필 수정 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ErrorResponse.of("프로필 수정 중 오류가 발생했습니다", "PROFILE_UPDATE_ERROR"));
        }
    }

    /**
     * 로그아웃
     * 클라이언트는 이 API 호출 후 자체적으로 토큰을 삭제해야 합니다.
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization") String authorization,
                                    HttpServletResponse response) {
        try {
            if (authorization != null && authorization.startsWith("Bearer ")) {
                // String token = authorization.substring(7);
                log.info("로그아웃 요청 처리 완료. 클라이언트는 토큰을 폐기해야 합니다.");
            }

            // Refresh token 쿠키 삭제
            authService.clearRefreshTokenCookie(response);

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("success", true);
            responseBody.put("message", "로그아웃되었습니다. 클라이언트에서 토큰을 삭제해주세요.");

            return ResponseEntity.ok(responseBody);

        } catch (Exception e) {
            log.error("로그아웃 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ErrorResponse.of("로그아웃 중 오류가 발생했습니다", "LOGOUT_ERROR"));
        }
    }


    /**
     * 계정 상태 확인
     */
    @GetMapping("/status")
    public ResponseEntity<?> checkAccountStatus(@RequestParam String email) {
        try {
            User user = userService.getUserByEmail(email);

            if (user == null) {
                return ResponseEntity.ok(Map.of(
                        "exists", false,
                        "message", "등록되지 않은 이메일입니다"
                ));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("exists", true);
            response.put("status", user.getStatus().name());
            response.put("providerType", user.getProviderType().name());
            response.put("message", "등록된 계정입니다");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("계정 상태 확인 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ErrorResponse.of("계정 상태 확인 중 오류가 발생했습니다", "ACCOUNT_STATUS_ERROR"));
        }
    }



    /**
     * 회원가입 (명세서: POST /api/auth/signup)
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@RequestBody SignupRequest signupRequest) {
        try {
            // 필수 입력값 검증
            if (signupRequest.getEmail() == null || signupRequest.getName() == null ||
                    signupRequest.getProviderId() == null || signupRequest.getProviderType() == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("40001", "필수 입력 코드가 누락되었습니다"));
            }

            // 이메일 중복 체크
            User existingUser = userService.getUserByEmail(signupRequest.getEmail());
            if (existingUser != null) {
                return ResponseEntity.status(409)
                        .body(ApiResponse.error("40901", "이미 존재하는 회원입니다."));
            }

            // providerId 중복 체크
            User existingProviderUser = userService.getUserByProviderId(signupRequest.getProviderId());
            if (existingProviderUser != null) {
                return ResponseEntity.status(409)
                        .body(ApiResponse.error("40901", "이미 존재하는 회원입니다."));
            }

            // 신규 사용자 생성
            User savedUser = userService.createUser(signupRequest);

            SignupResponse signupResponse = new SignupResponse(
                    savedUser.getUserId().toString(),
                    savedUser.getCreatedAt(),
                    savedUser.getUpdatedAt()
            );

            return ResponseEntity.ok(ApiResponse.success(signupResponse));

        } catch (Exception e) {
            log.error("회원가입 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("50001", "알 수 없는 서버 에러가 발생했습니다.(" + e.getMessage() + ")"));
        }
    }


    /**
     * 사용자 상태별 리다이렉트 URL 반환
     * - 신규회원 또는 개인 온보딩 미완료: /onboarding
     * - 개인 온보딩 완료, 커플 매칭 미완료: /coupleroom
     * - 개인 온보딩 및 커플 매칭 모두 완료: /home
     */
    @GetMapping("/redirect")
    public ResponseEntity<?> getUserRedirectUrl(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            if (userDetails == null) {
                return ResponseEntity.badRequest()
                        .body(ErrorResponse.of("인증된 사용자 정보를 찾을 수 없습니다", "UNAUTHORIZED"));
            }

            String providerId = userDetails.getUsername();
            User user = userService.validateUserByProviderId(providerId);

            // 개인 온보딩 완료 여부 확인
            boolean isOnboardingCompleted = userService.isOnboardingCompleted(user);
            
            // 커플 매칭 상태 확인
            boolean isCoupled = coupleService.isUserCoupled(providerId);

            String redirectUrl;
            String status;

            if (!isOnboardingCompleted) {
                // 신규회원 또는 개인 온보딩 미완료
                redirectUrl = onboardingRedirectUrl;
                status = RedirectStatus.ONBOARDING_REQUIRED;
            } else if (!isCoupled) {
                // 개인 온보딩 완료, 커플 매칭 미완료
                redirectUrl = coupleroomRedirectUrl;
                status = RedirectStatus.COUPLE_MATCHING_REQUIRED;
            } else {
                // 개인 온보딩 및 커플 매칭 모두 완료
                redirectUrl = homeRedirectUrl;
                status = RedirectStatus.COMPLETED;
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("redirectUrl", redirectUrl);
            response.put("status", status);
            response.put("isOnboardingCompleted", isOnboardingCompleted);
            response.put("isCoupled", isCoupled);

            log.info("사용자 리다이렉트 URL 반환 - userId: {}, status: {}, redirectUrl: {}", 
                    user.getUserId(), status, redirectUrl);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("사용자 리다이렉트 URL 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ErrorResponse.of("사용자 리다이렉트 URL 조회 중 오류가 발생했습니다", "REDIRECT_URL_ERROR"));
        }
    }
}
