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
        if (message.contains("access_denied")) {
            return "사용자가 로그인을 취소했습니다";
        } else if (message.contains("invalid_grant")) {
            return "인증 코드가 유효하지 않습니다";
        } else if (message.contains("invalid_client")) {
            return "클라이언트 인증에 실패했습니다";
        } else {
            return "로그인 중 오류가 발생했습니다";
        }
    }
}
