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

    // 인증 후 서비스 이용을 위한 최종 endpoint(?)
    @Transactional
    public AuthResponse processOAuth2Login(OAuth2UserInfo oAuth2UserInfo, String providerType) {
        try {
            String providerId = oAuth2UserInfo.getProviderId();
            String email = oAuth2UserInfo.getEmail();
            String name = oAuth2UserInfo.getName();

            // 1. 기존 사용자 조회 -> 보안 강화를 위해 2차 조회 진행
            User existingUser = userRepository.findByProviderId(providerId);
            boolean isNewUser = false;

            if (existingUser == null) {
                // 2. 신규 사용자 자동 회원가입
                log.info("신규 사용자 자동 회원가입 시작: providerId={}, email={}", providerId, email);
                
                // 이메일 중복 체크 -> 등록된 이메일인지 확인
                User emailUser = userRepository.findByEmail(email);
                if (emailUser != null) {
                    log.warn("이미 존재하는 이메일로 가입 시도: {}", email);
                    return new AuthResponse(
                            false,
                            "이미 가입된 이메일입니다",
                            null,
                            null,
                            null,
                            null
                    );
                }

                // 신규 사용자 생성
                User newUser = User.builder()
                        .providerType(ProviderType.valueOf(providerType.toUpperCase()))
                        .providerId(providerId)
                        .email(email)
                        .name(name)
                        .status(AccountStatus.ACTIVE)
                        .build();

                existingUser = userRepository.save(newUser);
                isNewUser = true;
                log.info("신규 사용자 회원가입 완료: userId={}, providerId={}", existingUser.getUserId(), providerId);
            } else {
                // 3. 기존 사용자 정보 업데이트 (선택적)
                if (!email.equals(existingUser.getEmail()) || !name.equals(existingUser.getName())) {
                    existingUser.updateUserInfo(email, name);
                    userRepository.save(existingUser);
                    log.info("기존 사용자 정보 업데이트: userId={}", existingUser.getUserId());
                }
            }

            // 4. 계정 상태 확인 -> 필요 없으면 삭제 가능
            if (existingUser.getStatus() != AccountStatus.ACTIVE) {
                log.warn("비활성화된 계정으로 로그인 시도: userId={}", existingUser.getUserId());
                return new AuthResponse(
                        false,
                        "비활성화된 계정입니다",
                        null,
                        null,
                        null,
                        null
                );
            }

            // 5. 사용자의 커플 정보 조회
            String coupleId = getCoupleIdByProviderId(existingUser.getProviderId());
            
            // 6. JWT 토큰 생성 (userId와 coupleId 포함)
            String accessToken = jwtUtil.createJwtWithUserIdAndCoupleId(
                existingUser.getProviderId(), 
                existingUser.getUserId(), 
                coupleId, 
                10 * 60 * 1000L
            ); // 10분
            String refreshToken = jwtUtil.createRefreshToken(existingUser.getProviderId());

            // 6. 사용자 정보 구성
            AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                    existingUser.getUserId().toString(),
                    existingUser.getEmail(),
                    existingUser.getName(),
                    existingUser.getProviderType().name(),
                    existingUser.getProviderId(),
                    existingUser.getStatus().name(),
                    isNewUser
            );

            // 7. 성공 응답 생성
            return new AuthResponse(
                    true,
                    isNewUser ? "회원가입 및 로그인 성공" : "로그인 성공",
                    accessToken,
                    refreshToken,
                    3600L, // 1시간 (초 단위)
                    userInfo
            );

        } catch (Exception e) {
            log.error("OAuth2 로그인 처리 중 오류 발생: {}", e.getMessage(), e);
            return new AuthResponse(
                    false,
                    "로그인 처리 중 오류가 발생했습니다",
                    null,
                    null,
                    null,
                    null
            );
        }
    }
    
    /**
     * 사용자의 커플 ID 조회
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
            log.info("리프레시 토큰 검증 시작: {}", refreshToken != null ? "토큰 존재" : "토큰 없음");
            
            if (refreshToken == null || refreshToken.trim().isEmpty()) {
                log.warn("리프레시 토큰이 null이거나 비어있음");
                return new AuthResponse(
                        false,
                        "리프레시 토큰이 없습니다",
                        null,
                        null,
                        null,
                        null
                );
            }
            
            // 리프레시 토큰 검증
            if (jwtUtil.isTokenExpired(refreshToken)) {
                log.warn("리프레시 토큰이 만료됨");
                return new AuthResponse(
                        false,
                        "리프레시 토큰이 만료되었습니다",
                        null,
                        null,
                        null,
                        null
                );
            }

            String providerId = jwtUtil.getUsername(refreshToken);
            log.info("리프레시 토큰에서 추출한 providerId: {}", providerId);
            
            User user = userRepository.findByProviderId(providerId);
            log.info("DB에서 조회한 사용자: {}", user != null ? user.getEmail() : "null");

            if (user == null || user.getStatus() != AccountStatus.ACTIVE) {
                log.warn("사용자가 존재하지 않거나 비활성 상태: providerId={}, user={}, status={}", 
                        providerId, user != null ? user.getEmail() : "null", 
                        user != null ? user.getStatus() : "null");
                return new AuthResponse(
                        false,
                        "유효하지 않은 사용자입니다",
                        null,
                        null,
                        null,
                        null
                );
            }

            // 새로운 액세스 토큰 생성 (userID 포함)
            String newAccessToken = jwtUtil.createJwtWithUserId(providerId, user.getUserId(), 10 * 60 * 1000L);
            String newRefreshToken = jwtUtil.createRefreshToken(providerId);

            AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                    user.getUserId().toString(),
                    user.getEmail(),
                    user.getName(),
                    user.getProviderType().name(),
                    user.getProviderId(),
                    user.getStatus().name(),
                    false
            );

            return new AuthResponse(
                    true,
                    "토큰 갱신 성공",
                    newAccessToken,
                    newRefreshToken,
                    3600L,
                    userInfo
            );

        } catch (Exception e) {
            log.error("토큰 갱신 중 오류 발생: {}", e.getMessage(), e);
            return new AuthResponse(
                    false,
                    "토큰 갱신 중 오류가 발생했습니다",
                    null,
                    null,
                    null,
                    null
            );
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
}
