package PitterPatter.loventure.authService.security;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;

import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
public class CookieOAuth2AuthorizationRequestRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {
    
    private static final String OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME = "oauth2_auth_request";
    private static final String REDIRECT_URI_PARAM_COOKIE_NAME = "redirect_uri";
    private static final int COOKIE_EXPIRE_SECONDS = 180; // 3분
    
    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        log.debug("OAuth2 인증 요청 로드 시도");
        
        try {
            Cookie cookie = getCookie(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
            if (cookie == null) {
                log.debug("OAuth2 인증 요청 쿠키를 찾을 수 없음");
                return null;
            }
            
            OAuth2AuthorizationRequest authRequest = deserialize(cookie.getValue(), OAuth2AuthorizationRequest.class);
            log.debug("OAuth2 인증 요청 로드 성공: {}", authRequest != null ? authRequest.getState() : "null");
            return authRequest;
            
        } catch (IOException | ClassNotFoundException e) {
            log.error("OAuth2 인증 요청 로드 중 오류 발생: {}", e.getMessage(), e);
            return null;
        }
    }
    
    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, 
                                       HttpServletRequest request, 
                                       HttpServletResponse response) {
        log.debug("OAuth2 인증 요청 저장 시도: {}", authorizationRequest != null ? authorizationRequest.getState() : "null");
        
        try {
            if (authorizationRequest == null) {
                log.debug("인증 요청이 null이므로 쿠키 삭제");
                removeAuthorizationRequestCookies(request, response);
                return;
            }
            
            // 인증 요청을 쿠키에 저장
            String serializedAuthRequest = serialize(authorizationRequest);
            addCookie(response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME, serializedAuthRequest, COOKIE_EXPIRE_SECONDS);
            
            // 리다이렉트 URI도 저장 (선택사항)
            String redirectUriAfterLogin = request.getParameter(REDIRECT_URI_PARAM_COOKIE_NAME);
            if (StringUtils.hasText(redirectUriAfterLogin)) {
                addCookie(response, REDIRECT_URI_PARAM_COOKIE_NAME, redirectUriAfterLogin, COOKIE_EXPIRE_SECONDS);
                log.debug("리다이렉트 URI 저장: {}", redirectUriAfterLogin);
            }
            
            log.debug("OAuth2 인증 요청 저장 완료");
            
        } catch (IOException e) {
            log.error("OAuth2 인증 요청 저장 중 오류 발생: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, 
                                                               HttpServletResponse response) {
        log.debug("OAuth2 인증 요청 제거 시도");
        
        OAuth2AuthorizationRequest authRequest = loadAuthorizationRequest(request);
        if (authRequest != null) {
            removeAuthorizationRequestCookies(request, response);
            log.debug("OAuth2 인증 요청 제거 완료");
        } else {
            log.debug("제거할 OAuth2 인증 요청이 없음");
        }
        
        return authRequest;
    }

    public void removeAuthorizationRequestCookies(HttpServletRequest request, HttpServletResponse response) {
        deleteCookie(response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
        deleteCookie(response, REDIRECT_URI_PARAM_COOKIE_NAME);
        log.debug("OAuth2 인증 요청 쿠키들 삭제 완료");
    }

    private void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(maxAge);
        cookie.setSecure(true);
        response.addCookie(cookie);
        log.debug("쿠키 추가: {} (maxAge: {})", name, maxAge);
    }
    

    private void deleteCookie(HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, null);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        log.debug("쿠키 삭제: {}", name);
    }

    private Cookie getCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return null;
        }
        
        for (Cookie cookie : request.getCookies()) {
            if (cookie.getName().equals(name)) {
                return cookie;
            }
        }
        return null;
    }

    private String serialize(Object obj) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
            return Base64.getUrlEncoder().encodeToString(baos.toByteArray());
        }
    }
    

    @SuppressWarnings("unchecked")
    private <T> T deserialize(String str, Class<T> clazz) throws IOException, ClassNotFoundException {
        byte[] data = Base64.getUrlDecoder().decode(str);
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            return (T) ois.readObject();
        }
    }
}
