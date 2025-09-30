package PitterPatter.loventure.authService.handler;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {
    // 로그인 실패 시 처리
    @Value("${spring.jwt.redirect.base}")
    private String REDIRECT_URI_BASE;
    
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException authenticationException) throws ServletException, IOException {
        log.error("OAuth2 로그인 실패: {}", authenticationException.getMessage(), authenticationException);

        String errorMessage = getErrorMessage(authenticationException);
        String encodedErrorMessage = URLEncoder.encode(errorMessage, StandardCharsets.UTF_8.toString());
        String redirectUrl = REDIRECT_URI_BASE + "/login-failure?error=" + encodedErrorMessage;
        
        log.info("로그인 실패로 리다이렉트: {}", redirectUrl);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
    
    private String getErrorMessage(AuthenticationException exception) {
        String message = exception.getMessage();
        log.info("OAuth2 오류 메시지 분석: {}", message);
        
        if (message.contains("access_denied")) {
            return "사용자가 로그인을 취소했습니다";
        } else if (message.contains("invalid_grant")) {
            return "인증 코드가 유효하지 않습니다";
        } else if (message.contains("invalid_client")) {
            return "클라이언트 인증에 실패했습니다";
        } else if (message.contains("authorization_request_not_found")) {
            return "인증 요청을 찾을 수 없습니다. 로그인 시간이 초과되었거나 브라우저 설정 문제일 수 있습니다. 다시 시도해주세요.";
        } else if (message.contains("invalid_request")) {
            return "잘못된 인증 요청입니다";
        } else if (message.contains("unsupported_response_type")) {
            return "지원하지 않는 응답 타입입니다";
        } else if (message.contains("invalid_scope")) {
            return "유효하지 않은 권한 범위입니다";
        } else if (message.contains("server_error")) {
            return "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요";
        } else if (message.contains("temporarily_unavailable")) {
            return "서비스가 일시적으로 사용할 수 없습니다. 잠시 후 다시 시도해주세요";
        } else {
            return "로그인 중 오류가 발생했습니다: " + message;
        }
    }
}
