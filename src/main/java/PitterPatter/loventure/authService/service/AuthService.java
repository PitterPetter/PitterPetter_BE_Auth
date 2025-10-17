package PitterPatter.loventure.authService.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import PitterPatter.loventure.authService.dto.OAuth2UserInfo;
import PitterPatter.loventure.authService.dto.response.AuthResponse;
import PitterPatter.loventure.authService.repository.AccountStatus;
import PitterPatter.loventure.authService.repository.CoupleRoom;
import PitterPatter.loventure.authService.repository.CoupleRoomRepository;
import PitterPatter.loventure.authService.repository.ProviderType;
import PitterPatter.loventure.authService.repository.User;
import PitterPatter.loventure.authService.repository.UserRepository;
import PitterPatter.loventure.authService.security.JWTUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final CoupleRoomRepository coupleRoomRepository;
    private final JWTUtil jwtUtil;

    @Value("${spring.jwt.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${spring.jwt.cookie.domain:localhost}")
    private String cookieDomain;

    /**
     * 인증 후 서비스 이용을 위한 최종 endpoint(?)
     */
    @Transactional
    public AuthResponse processOAuth2Login(OAuth2UserInfo oAuth2UserInfo, String providerType) {
        try {
            String providerId = oAuth2UserInfo.getProviderId();
            String email = oAuth2UserInfo.getEmail();
            String name = oAuth2UserInfo.getName();

            // 1. 기존 사용자 조회
            User existingUser = userRepository.findByProviderId(providerId);
            boolean isNewUser = false;

            if (existingUser == null) {
                // 신규 사용자 자동 회원가입 로직... (생략)
                User emailUser = userRepository.findByEmail(email);
                if (emailUser != null) {
                    log.warn("이미 존재하는 이메일로 가입 시도: {}", email);
                    return new AuthResponse(false, "이미 가입된 이메일입니다", null, null, null, null);
                }

                User newUser = User.builder()
                        .providerType(ProviderType.valueOf(providerType.toUpperCase()))
                        .providerId(providerId)
                        .email(email)
                        .name(name)
                        .status(AccountStatus.ACTIVE)
                        .build();

                existingUser = userRepository.save(newUser);
                isNewUser = true;
            } else {
                // 기존 사용자 정보 업데이트 로직... (생략)
                if (!email.equals(existingUser.getEmail()) || !name.equals(existingUser.getName())) {
                    existingUser.updateUserInfo(email, name);
                    userRepository.save(existingUser);
                }
            }

            // 계정 상태 확인 로직... (생략)
            if (existingUser.getStatus() != AccountStatus.ACTIVE) {
                return new AuthResponse(false, "비활성화된 계정입니다", null, null, null, null);
            }

            // 5. 사용자의 커플 정보 조회
            // [수정] CoupleService의 public 메서드를 호출하도록 변경
            String coupleId = getCoupleIdByProviderId(existingUser.getProviderId());

            // 6. JWT 토큰 생성
            String accessToken = jwtUtil.createJwtWithUserIdAndCoupleId(
                    existingUser.getProviderId(),
                    existingUser.getUserId(),
                    coupleId,
                    10 * 60 * 1000L
            );
            String refreshToken = jwtUtil.createRefreshToken(existingUser.getProviderId());

            // 7. [핵심 수정] DB에 Refresh Token 저장 및 사용자 엔티티 업데이트 (Stateful)
            existingUser.setRefreshToken(refreshToken);
            userRepository.save(existingUser);
            log.info("DB에 Refresh Token 저장 완료");

            // 8. 응답 구성 (생략)
            AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                    existingUser.getUserId().toString(),
                    existingUser.getEmail(),
                    existingUser.getName(),
                    existingUser.getProviderType().name(),
                    existingUser.getProviderId(),
                    existingUser.getStatus().name(),
                    isNewUser
            );

            return new AuthResponse(
                    true,
                    isNewUser ? "회원가입 및 로그인 성공" : "로그인 성공",
                    accessToken,
                    refreshToken,
                    3600L,
                    userInfo
            );

        } catch (Exception e) {
            log.error("OAuth2 로그인 처리 중 오류 발생: {}", e.getMessage(), e);
            return new AuthResponse(
                    false, "로그인 처리 중 오류가 발생했습니다", null, null, null, null);
        }
    }

    /**
     * 사용자의 커플 ID 조회 (private -> public 또는 CoupleService로 이동 권장)
     * AuthService 내부에서만 사용하기 때문에 private으로 유지하지만,
     * CoupleService에 종속성이 강한 로직이므로, 향후 CoupleService로 이동을 고려해야 합니다.
     */
    private String getCoupleIdByProviderId(String providerId) {
        try {
            // 사용자가 생성자이거나 파트너인 활성 상태의 커플룸 조회
            Optional<CoupleRoom> coupleRoomOpt = coupleRoomRepository.findByCreatorUserIdAndStatus(providerId, CoupleRoom.CoupleStatus.ACTIVE)
                    .or(() -> coupleRoomRepository.findByPartnerUserIdAndStatus(providerId, CoupleRoom.CoupleStatus.ACTIVE));

            if (coupleRoomOpt.isPresent()) {
                return coupleRoomOpt.get().getCoupleId();
            }

            return null; // 커플이 아닌 경우
        } catch (Exception e) {
            log.warn("커플 정보 조회 중 오류 발생: {}", e.getMessage());
            return null;
        }
    }

    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        try {
            // 1. 기본 유효성 검사 (null, 만료)
            if (refreshToken == null || refreshToken.trim().isEmpty() || jwtUtil.isTokenExpired(refreshToken)) {
                log.warn("리프레시 토큰이 유효하지 않거나 만료됨");
                return new AuthResponse(false, "리프레시 토큰이 유효하지 않거나 만료되었습니다", null, null, null, null);
            }

            String providerId = jwtUtil.getUsername(refreshToken);
            User user = userRepository.findByProviderId(providerId);

            // 2. 사용자 존재 및 활성 상태 확인
            if (user == null || user.getStatus() != AccountStatus.ACTIVE) {
                log.warn("유효하지 않거나 비활성 사용자: {}", providerId);
                return new AuthResponse(false, "유효하지 않은 사용자입니다", null, null, null, null);
            }

            // 3. [보안 검증] 요청된 Refresh Token이 DB에 저장된 토큰과 일치하는지 확인
            // User 엔티티의 refreshToken 필드를 사용합니다.
            if (user.getRefreshToken() == null || !user.getRefreshToken().equals(refreshToken)) {
                log.warn("DB와 Refresh Token 불일치 감지. 탈취 가능성. 토큰 무효화.");
                // DB의 토큰을 null로 설정하여 기존 세션 무효화
                user.setRefreshToken(null);
                userRepository.save(user);

                return new AuthResponse(false, "유효하지 않은 리프레시 토큰입니다 (불일치)", null, null, null, null);
            }

            // 4. 새로운 Access Token 및 Refresh Token 생성 (롤링)
            String coupleId = getCoupleIdByProviderId(providerId); // Couple ID 재조회
            String newAccessToken = jwtUtil.createJwtWithUserIdAndCoupleId(providerId, user.getUserId(), coupleId, 10 * 60 * 1000L);
            String newRefreshToken = jwtUtil.createRefreshToken(providerId);

            // 5. [핵심 수정] 새로운 Refresh Token을 DB에 저장
            user.setRefreshToken(newRefreshToken);
            userRepository.save(user);
            log.info("DB에 새로운 Refresh Token 저장 및 갱신 완료");

            // 6. 응답 구성 (생략)
            AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                    user.getUserId().toString(), user.getEmail(), user.getName(), user.getProviderType().name(),
                    user.getProviderId(), user.getStatus().name(), false);

            return new AuthResponse(
                    true, "토큰 갱신 성공", newAccessToken, newRefreshToken, 3600L, userInfo);

        } catch (Exception e) {
            log.error("토큰 갱신 중 오류 발생: {}", e.getMessage(), e);
            return new AuthResponse(false, "토큰 갱신 중 오류가 발생했습니다", null, null, null, null);
        }
    }
    /**
     * 쿠키에서 refresh token 추출
     */
    public String getRefreshTokenFromCookie(HttpServletRequest request) {
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
    public void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie("refresh_token", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure); // 환경별 설정 사용
        cookie.setPath("/");
        cookie.setDomain(cookieDomain);
        cookie.setMaxAge(14 * 24 * 60 * 60); // 14일
        response.addCookie(cookie);
        log.info("Refresh token 쿠키 설정 완료 - Domain: {}, Secure: {}", cookieDomain, cookieSecure);
    }

    /**
     * Refresh token 쿠키 삭제
     */
    public void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("refresh_token", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure); // 환경별 설정 사용
        cookie.setPath("/");
        cookie.setDomain(cookieDomain); // 환경별 도메인 설정
        cookie.setMaxAge(0); // 즉시 삭제
        response.addCookie(cookie);
        log.info("Refresh token 쿠키 삭제 완료 - Domain: {}, Secure: {}", cookieDomain, cookieSecure);
    }

    /**
     * JWT 토큰 검증 (Territory-service용)
     */
    public boolean verifyJwtToken(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                log.warn("빈 JWT 토큰");
                return false;
            }

            // JWT 토큰 유효성 검사
            if (jwtUtil.isTokenExpired(token)) {
                log.warn("만료된 JWT 토큰");
                return false;
            }

            // 토큰에서 사용자 정보 추출
            String providerId = jwtUtil.getUsername(token);
            if (providerId == null || providerId.trim().isEmpty()) {
                log.warn("유효하지 않은 JWT 토큰 - providerId 없음");
                return false;
            }

            // 사용자 존재 및 활성 상태 확인
            User user = userRepository.findByProviderId(providerId);
            if (user == null || user.getStatus() != AccountStatus.ACTIVE) {
                log.warn("유효하지 않거나 비활성 사용자: {}", providerId);
                return false;
            }

            log.info("JWT 토큰 검증 성공 - providerId: {}", providerId);
            return true;

        } catch (Exception e) {
            log.error("JWT 토큰 검증 중 오류 발생: {}", e.getMessage(), e);
            return false;
        }
    }
}
