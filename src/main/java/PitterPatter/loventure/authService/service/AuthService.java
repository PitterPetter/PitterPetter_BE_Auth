package PitterPatter.loventure.authService.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import PitterPatter.loventure.authService.dto.response.AuthResponse;
import PitterPatter.loventure.authService.dto.OAuth2UserInfo;
import PitterPatter.loventure.authService.repository.AccountStatus;
import PitterPatter.loventure.authService.repository.ProviderType;
import PitterPatter.loventure.authService.repository.User;
import PitterPatter.loventure.authService.repository.UserRepository;
import PitterPatter.loventure.authService.security.JWTUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final JWTUtil jwtUtil;

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
                    return AuthResponse.builder()
                            .success(false)
                            .message("이미 가입된 이메일입니다")
                            .build();
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
                return AuthResponse.builder()
                        .success(false)
                        .message("비활성화된 계정입니다")
                        .build();
            }

            // 5. JWT 토큰 생성 (userID 포함)
            String accessToken = jwtUtil.createJwtWithUserId(existingUser.getProviderId(), existingUser.getUserId(), 60 * 60 * 1000L); // 1시간
            String refreshToken = jwtUtil.createRefreshToken(existingUser.getProviderId());

            // 6. 사용자 정보 구성
            AuthResponse.UserInfo userInfo = AuthResponse.UserInfo.builder()
                    .userId(existingUser.getUserId())
                    .email(existingUser.getEmail())
                    .name(existingUser.getName())
                    .providerType(existingUser.getProviderType().name())
                    .providerId(existingUser.getProviderId())
                    .status(existingUser.getStatus().name())
                    .isNewUser(isNewUser)
                    .build();

            // 7. 성공 응답 생성
            return AuthResponse.builder()
                    .success(true)
                    .message(isNewUser ? "회원가입 및 로그인 성공" : "로그인 성공")
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .expiresIn(3600L) // 1시간 (초 단위)
                    .user(userInfo)
                    .build();

        } catch (Exception e) {
            log.error("OAuth2 로그인 처리 중 오류 발생: {}", e.getMessage(), e);
            return AuthResponse.builder()
                    .success(false)
                    .message("로그인 처리 중 오류가 발생했습니다")
                    .build();
        }
    }

    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        try {
            // 리프레시 토큰 검증
            if (jwtUtil.isTokenExpired(refreshToken)) {
                return AuthResponse.builder()
                        .success(false)
                        .message("리프레시 토큰이 만료되었습니다")
                        .build();
            }

            String providerId = jwtUtil.getUsername(refreshToken);
            User user = userRepository.findByProviderId(providerId);

            if (user == null || user.getStatus() != AccountStatus.ACTIVE) {
                return AuthResponse.builder()
                        .success(false)
                        .message("유효하지 않은 사용자입니다")
                        .build();
            }

            // 새로운 액세스 토큰 생성 (userID 포함)
            String newAccessToken = jwtUtil.createJwtWithUserId(providerId, user.getUserId(), 60 * 60 * 1000L);
            String newRefreshToken = jwtUtil.createRefreshToken(providerId);

            AuthResponse.UserInfo userInfo = AuthResponse.UserInfo.builder()
                    .userId(user.getUserId())
                    .email(user.getEmail())
                    .name(user.getName())
                    .providerType(user.getProviderType().name())
                    .providerId(user.getProviderId())
                    .status(user.getStatus().name())
                    .isNewUser(false)
                    .build();

            return AuthResponse.builder()
                    .success(true)
                    .message("토큰 갱신 성공")
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .expiresIn(3600L)
                    .user(userInfo)
                    .build();

        } catch (Exception e) {
            log.error("토큰 갱신 중 오류 발생: {}", e.getMessage(), e);
            return AuthResponse.builder()
                    .success(false)
                    .message("토큰 갱신 중 오류가 발생했습니다")
                    .build();
        }
    }
}
