package PitterPatter.loventure.authService.handler;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import PitterPatter.loventure.authService.dto.AuthResponse;
import PitterPatter.loventure.authService.dto.GoogleUserInfo;
import PitterPatter.loventure.authService.dto.KakaoUserInfo;
import PitterPatter.loventure.authService.dto.OAuth2UserInfo;
import PitterPatter.loventure.authService.repository.UserRepository;
import PitterPatter.loventure.authService.security.JWTUtil;
import PitterPatter.loventure.authService.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    // CustomOAuth2UserService 실행 이후 호출
    // JWT를 발급 및 FE에 전달하는 역할
    private final JWTUtil jwtUtil;
    private final UserRepository userRepository;
    private final AuthService authService;

    // 1. @Value 어노테이션으로 리다이렉트 URI 변수 선언 (추가)
    @Value("${spring.jwt.redirect.base}")
    private String REDIRECT_URI_BASE;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        // 로그인한 사용자의 정보를 가져오는 method
        try {
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
            
            if (oAuth2User == null) {
                log.error("OAuth2User 객체가 null입니다");
                redirectToFailure(request, response, "인증 정보를 가져올 수 없습니다");
                return;
            }

            // CustomOAuth2UserService에서 반환한 Name(providerId)을 사용
            String providerId = authentication.getName();
            
            if (providerId == null || providerId.isEmpty()) {
                log.error("providerId가 null이거나 비어있습니다");
                redirectToFailure(request, response, "사용자 식별 정보가 없습니다");
                return;
            }
            
            log.info("OAuth2 로그인 성공 - providerId: {}", providerId);
            
            // OAuth2User에서 제공자 정보 추출
            String registrationId = getRegistrationId(request);
            OAuth2UserInfo oAuth2UserInfo = createOAuth2UserInfo(oAuth2User, registrationId);
            
            if (oAuth2UserInfo == null) {
                log.error("지원하지 않는 OAuth2 제공자: {}", registrationId);
                redirectToFailure(request, response, "지원하지 않는 로그인 방식입니다");
                return;
            }

            // AuthService를 통한 로그인/회원가입 처리
            AuthResponse authResponse = authService.processOAuth2Login(oAuth2UserInfo, registrationId);
            
            if (!authResponse.isSuccess()) {
                log.error("OAuth2 로그인 처리 실패: {}", authResponse.getMessage());
                redirectToFailure(request, response, authResponse.getMessage());
                return;
            }

            // 성공 시 리다이렉트 URL 구성
            String targetUrl = buildSuccessRedirectUrl(authResponse);
            
            log.info("OAuth2 로그인 성공 - 리다이렉트: {}, 신규가입: {}", targetUrl, authResponse.getUser().isNewUser());
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
            
        } catch (IOException | RuntimeException e) {
            log.error("OAuth2 로그인 성공 처리 중 오류 발생: {}", e.getMessage(), e);
            try {
                redirectToFailure(request, response, "로그인 처리 중 오류가 발생했습니다");
            } catch (IOException ioException) {
                log.error("실패 리다이렉트 중 오류 발생: {}", ioException.getMessage(), ioException);
            }
        }
    }
    
    // 위의 providerTYPE 확인을 위해 URI의 정보로 provider 반환
    private String getRegistrationId(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        if (requestURI.contains("/oauth2/authorization/google")) {
            return "google";
        } else if (requestURI.contains("/oauth2/authorization/kakao")) {
            return "kakao";
        }
        return "unknown";
    }
    
    private OAuth2UserInfo createOAuth2UserInfo(OAuth2User oAuth2User, String registrationId) {
        switch (registrationId.toLowerCase()) {
            case "google":
                return new GoogleUserInfo(oAuth2User.getAttributes());
            case "kakao":
                return new KakaoUserInfo(oAuth2User.getAttributes());
            default:
                return null;
        }
    }
    
    // Access token/refresh token, useId, 신규 가입 유무 등 사용자의 정보를 이용해
    // 리다이렉트 URL 생성 -> FE에서 읽어서 로그인 후 화면으로 이동
    private String buildSuccessRedirectUrl(AuthResponse authResponse) {
        StringBuilder url = new StringBuilder(REDIRECT_URI_BASE);
        url.append("/social-login?");
        url.append("success=true");
        url.append("&access_token=").append(authResponse.getAccessToken());
        url.append("&refresh_token=").append(authResponse.getRefreshToken());
        url.append("&expires_in=").append(authResponse.getExpiresIn());
        url.append("&is_new_user=").append(authResponse.getUser().isNewUser());
        url.append("&user_id=").append(authResponse.getUser().getUserId());
        url.append("&email=").append(authResponse.getUser().getEmail());
        url.append("&name=").append(authResponse.getUser().getName());
        return url.toString();
    }
    
    private void redirectToFailure(HttpServletRequest request, HttpServletResponse response, String errorMessage) throws IOException {
        String failureUrl = REDIRECT_URI_BASE + "/login-failure?error=" + errorMessage;
        log.info("로그인 실패로 리다이렉트: {}", failureUrl);
        getRedirectStrategy().sendRedirect(request, response, failureUrl);
    }
}