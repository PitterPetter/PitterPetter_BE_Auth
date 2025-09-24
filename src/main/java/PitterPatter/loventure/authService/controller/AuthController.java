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

import PitterPatter.loventure.authService.dto.response.ApiResponse;
import PitterPatter.loventure.authService.dto.response.AuthResponse;
import PitterPatter.loventure.authService.dto.response.ErrorResponse;
import PitterPatter.loventure.authService.dto.request.LoginRequest;
import PitterPatter.loventure.authService.dto.response.LoginResponse;
import PitterPatter.loventure.authService.dto.request.SignupRequest;
import PitterPatter.loventure.authService.dto.response.SignupResponse;
import PitterPatter.loventure.authService.repository.User;
import PitterPatter.loventure.authService.repository.UserRepository;
import PitterPatter.loventure.authService.security.JWTUtil;
import PitterPatter.loventure.authService.service.AuthService;
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
     * 토큰 갱신
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestParam String refreshToken) {
        try {
            AuthResponse response = authService.refreshToken(refreshToken);
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            log.error("토큰 갱신 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ErrorResponse.of("토큰 갱신 중 오류가 발생했습니다", "REFRESH_TOKEN_ERROR"));
        }
    }

    /**
     * [리팩토링] 사용자 정보 조회
     * @AuthenticationPrincipal을 사용하여 JWTFilter가 검증하고 SecurityContext에 저장한
     * 인증된 사용자 정보를 직접 주입받습니다.
     * 컨트롤러에서 토큰을 직접 파싱하고 검증하는 중복 로직을 제거했습니다.
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
                    .userId(user.getUserId())
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
     * [수정] 로그아웃
     * 계정을 비활성화하는 대신, stateless JWT 방식에 맞는 로그아웃을 구현합니다.
     * 클라이언트는 이 API 호출 후 자체적으로 토큰을 삭제해야 합니다.
     * (선택적 심화) 서버에서는 전달받은 토큰을 블랙리스트에 추가하여,
     * 해당 토큰이 만료되기 전까지 재사용될 수 없도록 처리할 수 있습니다.
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization") String authorization) {
        try {
            if (authorization != null && authorization.startsWith("Bearer ")) {
                // String token = authorization.substring(7);
                // (선택적 심화) 토큰 블랙리스트 처리 로직
                // logoutTokenService.blacklistToken(token);
                log.info("로그아웃 요청 처리 완료. 클라이언트는 토큰을 폐기해야 합니다.");
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "로그아웃되었습니다. 클라이언트에서 토큰을 삭제해주세요.");

            return ResponseEntity.ok(response);

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
     */
    @PostMapping("/login/{provider}")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @PathVariable String provider,
            @RequestBody LoginRequest loginRequest) {
        try {
            // provider 유효성 검사
            if (!provider.equals("google") && !provider.equals("kakao")) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("40007", "잘못된 " + provider + " 값입니다."));
            }

            // OAuth2 provider에서 사용자 정보 조회 (실제 구현에서는 provider API 호출)
            // 여기서는 간단히 예시로 처리
            // String providerAccessToken = loginRequest.getAccess_token();
            
            // 실제로는 provider API를 호출하여 사용자 정보를 가져와야 함
            // 예시: Google/Kakao API 호출하여 사용자 정보 조회
            
            // 임시 사용자 정보 (실제로는 provider API에서 가져옴)
            String email = "user@example.com";
            String name = "홍길동";
            String providerId = provider + "_" + System.currentTimeMillis();
            
            // 기존 사용자 조회 또는 신규 사용자 생성
            User user = userRepository.findByProviderId(providerId);
            boolean isNewUser = false;
            
            if (user == null) {
                // 신규 사용자 생성
                user = User.builder()
                        .providerType(PitterPatter.loventure.authService.repository.ProviderType.valueOf(provider.toUpperCase()))
                        .providerId(providerId)
                        .email(email)
                        .name(name)
                        .status(PitterPatter.loventure.authService.repository.AccountStatus.ACTIVE)
                        .build();
                user = userRepository.save(user);
                isNewUser = true;
            }

            // JWT 토큰 생성 (userID 포함)
            String accessToken = jwtUtil.createJwtWithUserId(user.getProviderId(), user.getUserId(), 60 * 60 * 1000L);
            String refreshToken = jwtUtil.createRefreshToken(user.getProviderId());

            LoginResponse loginResponse = LoginResponse.builder()
                    .user_id(user.getTsid().toString())
                    .isNewUser(isNewUser)
                    .email(user.getEmail())
                    .name(user.getName())
                    .JWT(accessToken)
                    .refresh_token(refreshToken)
                    .build();

            return ResponseEntity.ok(ApiResponse.success(loginResponse));

        } catch (Exception e) {
            log.error("로그인 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("50001", "알 수 없는 서버 에러가 발생했습니다.(" + e.getMessage() + ")"));
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
