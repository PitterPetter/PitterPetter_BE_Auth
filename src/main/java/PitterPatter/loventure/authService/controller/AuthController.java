package PitterPatter.loventure.authService.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import PitterPatter.loventure.authService.dto.request.LoginRequest;
import PitterPatter.loventure.authService.dto.request.SignupRequest;
import PitterPatter.loventure.authService.dto.response.ApiResponse;
import PitterPatter.loventure.authService.dto.response.AuthResponse;
import PitterPatter.loventure.authService.dto.response.ErrorResponse;
import PitterPatter.loventure.authService.dto.response.LoginResponse;
import PitterPatter.loventure.authService.dto.response.SignupResponse;
import PitterPatter.loventure.authService.repository.User;
import PitterPatter.loventure.authService.repository.UserRepository;
import PitterPatter.loventure.authService.security.JWTUtil;
import PitterPatter.loventure.authService.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final JWTUtil jwtUtil;
    /**
     * 토큰 갱신 (쿠키에서 refresh token 읽기)
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        try {
            // 쿠키에서 refresh token 추출
            String refreshToken = getRefreshTokenFromCookie(request);
            if (refreshToken == null) {
                return ResponseEntity.badRequest()
                        .body(ErrorResponse.of("리프레시 토큰이 없습니다", "REFRESH_TOKEN_NOT_FOUND"));
            }

            AuthResponse authResponse = authService.refreshToken(refreshToken);
            if (authResponse.isSuccess()) {
                // 새로운 refresh token을 쿠키에 저장
                setRefreshTokenCookie(response, authResponse.getRefreshToken());
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
     * 쿠키에서 refresh token 추출
     */
    private String getRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("refresh_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * Refresh token을 HttpOnly 쿠키에 저장
     */
    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie("refresh_token", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(14 * 24 * 60 * 60); // 14일
        response.addCookie(cookie);
    }

    /**
     * 사용자 정보 조회
     * @AuthenticationPrincipal을 사용하여 JWTFilter가 검증하고 SecurityContext에 저장한
     * 인증된 사용자 정보를 직접 주입받습니다.
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            if (userDetails == null) {
                return ResponseEntity.badRequest()
                        .body(ErrorResponse.of("인증된 사용자 정보를 찾을 수 없습니다", "UNAUTHORIZED"));
            }

            String providerId = userDetails.getUsername();
            User user = userRepository.findByProviderId(providerId);

            if (user == null) {
                return ResponseEntity.badRequest()
                        .body(ErrorResponse.of("사용자를 찾을 수 없습니다", "USER_NOT_FOUND"));
            }

            AuthResponse.UserInfo userInfo = AuthResponse.UserInfo.builder()
                    .userId(user.getTsid())
                    .email(user.getEmail())
                    .name(user.getName())
                    .providerType(user.getProviderType().name())
                    .providerId(user.getProviderId())
                    .status(user.getStatus().name())
                    .isNewUser(false)
                    .build();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("user", userInfo);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("사용자 정보 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ErrorResponse.of("사용자 정보 조회 중 오류가 발생했습니다", "USER_INFO_ERROR"));
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
            clearRefreshTokenCookie(response);

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
     * Refresh token 쿠키 삭제
     */
    private void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("refresh_token", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0); // 즉시 삭제
        response.addCookie(cookie);
        log.info("Refresh token 쿠키 삭제 완료");
    }

    /**
     * 계정 상태 확인
     */
    @GetMapping("/status")
    public ResponseEntity<?> checkAccountStatus(@RequestParam String email) {
        try {
            User user = userRepository.findByEmail(email);

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
     * OAuth2 로그인 (명세서: POST /api/auth/login/{provider})
     * 주의: 실제 OAuth2 로그인은 Spring Security OAuth2를 통해 처리됩니다.
     * 이 엔드포인트는 테스트용이거나 특별한 경우에만 사용됩니다.
     *
     * 실제 OAuth2 로그인 URL:
     * - Google: /oauth2/authorization/google
     * - Kakao: /oauth2/authorization/kakao
     */
    @PostMapping("/login/{provider}")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @PathVariable String provider,
            @RequestBody LoginRequest loginRequest) {

        return ResponseEntity.badRequest()
                .body(ApiResponse.error("40001",
                        "OAuth2 로그인은 Spring Security OAuth2를 통해 처리됩니다. " +
                                "다음 URL을 사용하세요: /oauth2/authorization/" + provider));
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
            User existingUser = userRepository.findByEmail(signupRequest.getEmail());
            if (existingUser != null) {
                return ResponseEntity.status(409)
                        .body(ApiResponse.error("40901", "이미 존재하는 회원입니다."));
            }

            // providerId 중복 체크
            User existingProviderUser = userRepository.findByProviderId(signupRequest.getProviderId());
            if (existingProviderUser != null) {
                return ResponseEntity.status(409)
                        .body(ApiResponse.error("40901", "이미 존재하는 회원입니다."));
            }

            // 신규 사용자 생성
            User newUser = User.builder()
                    .providerType(signupRequest.getProviderType())
                    .providerId(signupRequest.getProviderId())
                    .email(signupRequest.getEmail())
                    .name(signupRequest.getName())
                    .status(PitterPatter.loventure.authService.repository.AccountStatus.ACTIVE)
                    .build();

            User savedUser = userRepository.save(newUser);

            SignupResponse signupResponse = SignupResponse.builder()
                    .userId(savedUser.getTsid().toString())
                    .createdAt(savedUser.getCreatedAt())
                    .updatedAt(savedUser.getUpdatedAt())
                    .build();

            return ResponseEntity.ok(ApiResponse.success(signupResponse));

        } catch (Exception e) {
            log.error("회원가입 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("50001", "알 수 없는 서버 에러가 발생했습니다.(" + e.getMessage() + ")"));
        }
    }
}
