package PitterPatter.loventure.authService.security;

import java.io.IOException;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import PitterPatter.loventure.authService.repository.User;
import PitterPatter.loventure.authService.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class JWTFilter extends OncePerRequestFilter {
    // 로그인 후 인증 확인
    private final JWTUtil jwtUtil;
    private final UserRepository userRepository; // DB에서 사용자 정보를 조회하기 위함

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        // 요청의 header를 검사해서 token 확인
        String authorization = request.getHeader("Authorization");

        // token이 없으면 로그인을 하지 않은 사용자로 처리
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // token 발견 시, JWTUtil 내의 isTokenExpired method를 불러와서
        // 유효 기간 확인
        String token = null;
        try {
            // Authorization 헤더 형식 검증
            if (authorization.split(" ").length < 2) {
                log.warn("잘못된 Authorization 헤더 형식: {}", authorization);
                filterChain.doFilter(request, response);
                return;
            }
            
            token = authorization.split(" ")[1];
            if (token == null || token.trim().isEmpty()) {
                log.warn("빈 토큰 값");
                filterChain.doFilter(request, response);
                return;
            }
            
            log.info("JWT 토큰 추출: {}", token);

            // 토큰 만료 확인
            if (jwtUtil.isTokenExpired(token)) {
                log.warn("만료된 JWT 토큰: {}", token);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"error\":\"JWT 토큰이 만료되었습니다\",\"code\":\"JWT_TOKEN_EXPIRED\"}");
                return;
            }

            String providerId = jwtUtil.getUsername(token); // 토큰에서 providerId 추출
            log.info("JWT에서 추출한 providerId: {}", providerId);

            // providerId로 DB에서 사용자 조회
            User user = userRepository.findByProviderId(providerId);
            log.info("DB에서 조회한 사용자: {}", user != null ? user.getEmail() : "null");
            
            // 사용자가 존재하지 않는 경우 처리 -> 탈퇴 시 고려
            if (user == null) {
                log.warn("JWT 토큰에 해당하는 사용자를 찾을 수 없음: {}", providerId);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\":\"사용자를 찾을 수 없습니다\",\"code\":\"USER_NOT_FOUND\"}");
                return;
            }
            
            // 사용자 계정 상태 확인
            if (user.getStatus() == null || !user.getStatus().name().equals("ACTIVE")) {
                log.warn("비활성화된 사용자 계정: {}", providerId);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\":\"비활성화된 계정입니다\",\"code\":\"ACCOUNT_INACTIVE\"}");
                return;
            }

            // UserDetails 객체 생성
            UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                    .username(user.getProviderId())
                    .password("") // password는 사용하지 않으므로 비워둠
                    .authorities("ROLE_USER") // 권한 설정
                    .build();

            // MSA Gateway를 위한 헤더 추가
            response.setHeader("X-User-Id", providerId);
            
            // JWT에서 coupleId 추출
            String coupleId = jwtUtil.getCoupleIdFromToken(token);
            response.setHeader("X-Couple-Id", coupleId != null ? coupleId : "null");

            // SecurityContext에 인증 정보 저장
            Authentication authToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authToken);

        } catch (io.jsonwebtoken.security.SignatureException e) {
            log.error("JWT 서명 검증 실패: {} - 토큰: {}", e.getMessage(), token);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"JWT 서명이 유효하지 않습니다\",\"code\":\"INVALID_JWT_SIGNATURE\"}");
            return;
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.error("JWT 토큰 만료: {} - 토큰: {}", e.getMessage(), token);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"JWT 토큰이 만료되었습니다\",\"code\":\"JWT_TOKEN_EXPIRED\"}");
            return;
        } catch (IllegalArgumentException e) {
            log.error("JWT 토큰 형식 오류: {}", e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"JWT 토큰 형식이 잘못되었습니다\",\"code\":\"INVALID_JWT_FORMAT\"}");
            return;
        } catch (SecurityException e) {
            log.error("JWT 보안 오류: {}", e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"JWT 보안 검증 실패\",\"code\":\"JWT_SECURITY_ERROR\"}");
            return;
        } catch (IOException e) {
            log.error("JWT 응답 작성 오류: {}", e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"서버 내부 오류\",\"code\":\"INTERNAL_SERVER_ERROR\"}");
            return;
        } catch (Exception e) {
            log.error("JWT 토큰 파싱 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"JWT 토큰이 유효하지 않습니다\",\"code\":\"INVALID_JWT_TOKEN\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}