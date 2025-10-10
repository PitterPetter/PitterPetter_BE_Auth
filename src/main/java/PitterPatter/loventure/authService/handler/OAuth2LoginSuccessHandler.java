package PitterPatter.loventure.authService.handler;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import PitterPatter.loventure.authService.dto.GoogleUserInfo;
import PitterPatter.loventure.authService.dto.KakaoUserInfo;
import PitterPatter.loventure.authService.dto.OAuth2UserInfo;
import PitterPatter.loventure.authService.dto.response.AuthResponse;
import PitterPatter.loventure.authService.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    // [유지] JWT 생성 및 사용자 처리를 위해 필요
    private final AuthService authService;

    // [유지] 프론트엔드 기본 리다이렉트 경로
    @Value("${spring.jwt.redirect.base}")
    private String REDIRECT_URI_BASE;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        try {
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

            if (oAuth2User == null) {
                log.error("OAuth2User 객체가 null입니다");
                redirectToFailure(request, response, "인증 정보를 가져올 수 없습니다");
                return;
            }

            String providerId = authentication.getName();

            if (providerId == null || providerId.isEmpty()) {
                log.error("providerId가 null이거나 비어있습니다");
                redirectToFailure(request, response, "사용자 식별 정보가 없습니다");
                return;
            }

            log.info("OAuth2 로그인 성공 - providerId: {}", providerId);

            String registrationId = getRegistrationId(request);
            OAuth2UserInfo oAuth2UserInfo = createOAuth2UserInfo(oAuth2User, registrationId);

            if (oAuth2UserInfo == null) {
                log.error("지원하지 않는 OAuth2 제공자: {}", registrationId);
                redirectToFailure(request, response, "지원하지 않는 로그인 방식입니다");
                return;
            }

            // JWT 생성 및 사용자 상태 처리
            AuthResponse authResponse = authService.processOAuth2Login(oAuth2UserInfo, registrationId);

            if (!authResponse.success()) {
                log.error("OAuth2 로그인 처리 실패: {}", authResponse.message());
                redirectToFailure(request, response, authResponse.message());
                return;
            }

            // [수정] 상태 분기 로직 제거: 토큰을 전달할 단일 URL 생성
            String redirectUrl = buildSuccessRedirectUrl(authResponse);
            log.info("OAuth2 로그인 성공 - 토큰 전달 URL: {}", redirectUrl);
            getRedirectStrategy().sendRedirect(request, response, redirectUrl);

        } catch (IOException | RuntimeException e) {
            log.error("OAuth2 로그인 성공 처리 중 오류 발생: {}", e.getMessage(), e);
            try {
                redirectToFailure(request, response, "로그인 처리 중 오류가 발생했습니다");
            } catch (IOException ioException) {
                log.error("실패 리다이렉트 중 오류 발생: {}", ioException.getMessage(), ioException);
            }
        }
    }

    // 위의 providerTYPE 확인을 위해 URI의 정보로 provider 반환 (로직 변경 없음)
    private String getRegistrationId(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.info("OAuth2 콜백 URI: {}", requestURI);

        if (requestURI.equals("/login/oauth2/code/google") || requestURI.contains("/login/oauth2/code/google")) {
            log.info("Google OAuth2 콜백 감지");
            return "google";
        } else if (requestURI.equals("/login/oauth2/code/kakao") || requestURI.contains("/login/oauth2/code/kakao")) {
            log.info("Kakao OAuth2 콜백 감지");
            return "kakao";
        }

        // OAuth2 인증 시작 URL에서 provider 정보 추출
        if (requestURI.contains("/oauth2/authorization/google")) {
            log.info("Google OAuth2 인증 시작 감지");
            return "google";
        } else if (requestURI.contains("/oauth2/authorization/kakao")) {
            log.info("Kakao OAuth2 인증 시작 감지");
            return "kakao";
        }

        // Referer 헤더에서 provider 정보 추출
        String referer = request.getHeader("Referer");
        log.info("Referer 헤더: {}", referer);
        if (referer != null) {
            if (referer.contains("google")) {
                log.info("Referer에서 Google 감지");
                return "google";
            } else if (referer.contains("kakao")) {
                log.info("Referer에서 Kakao 감지");
                return "kakao";
            }
        }

        // OAuth2User의 attributes에서 provider 정보 추출
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof OAuth2User) {
                OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
                log.info("OAuth2User attributes: {}", oAuth2User.getAttributes().keySet());

                // Google의 경우 sub 필드가 있고, Kakao의 경우 id 필드가 있음
                if (oAuth2User.getAttributes().containsKey("sub")) {
                    log.info("OAuth2User attributes에서 Google 감지");
                    return "google";
                } else if (oAuth2User.getAttributes().containsKey("id")) {
                    log.info("OAuth2User attributes에서 Kakao 감지");
                    return "kakao";
                }
            }
        } catch (Exception e) {
            log.warn("OAuth2User에서 provider 정보 추출 실패: {}", e.getMessage());
        }

        log.warn("Provider 감지 실패 - URI: {}, Referer: {}", requestURI, referer);
        return "unknown";
    }

    private OAuth2UserInfo createOAuth2UserInfo(OAuth2User oAuth2User, String registrationId) {
        return switch (registrationId.toLowerCase()) {
            case "google" -> new GoogleUserInfo(oAuth2User.getAttributes());
            case "kakao" -> new KakaoUserInfo(oAuth2User.getAttributes());
            default -> null;
        };
    }

    // [수정] 상태 분기 로직 제거: 토큰을 전달할 단일 URL 생성
    private String buildSuccessRedirectUrl(AuthResponse authResponse) {
        StringBuilder url = new StringBuilder();

        // 1. 프론트엔드 기본 URL로 리다이렉트
        url.append(REDIRECT_URI_BASE);

        // 2. 쿼리 파라미터로 Access Token과 isNewUser 정보 전달
        url.append(REDIRECT_URI_BASE.contains("?") ? "&" : "?");
        url.append("access_token=").append(authResponse.accessToken());

        // is_new_user 정보는 프론트엔드에서 초기 처리에 유용하므로 유지
        url.append("&is_new_user=").append(authResponse.user().isNewUser());

        return url.toString();
    }

    private void redirectToFailure(HttpServletRequest request, HttpServletResponse response, String errorMessage) throws IOException {
        String redirectUrl = REDIRECT_URI_BASE + "/login?error=" +
                URLEncoder.encode(errorMessage, StandardCharsets.UTF_8.toString());
        log.info("OAuth2 로그인 실패 - 리다이렉트 URL: {}", redirectUrl);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

}