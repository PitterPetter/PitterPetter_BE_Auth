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
import PitterPatter.loventure.authService.service.CoupleService;
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
    private final CoupleService coupleService; // 커플 정보 조회를 위함

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
        try {
            String token = authorization.split(" ")[1];
            log.info("JWT 토큰 추출: {}", token);

            // 토큰 만료 확인
            if (jwtUtil.isTokenExpired(token)) {
                log.warn("만료된 JWT 토큰: {}", token);
                filterChain.doFilter(request, response);
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
            response.setHeader("X-Couple-Id", "null");

            // SecurityContext에 인증 정보 저장
            Authentication authToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authToken);

        } catch (IllegalArgumentException | SecurityException | IOException e) {
            log.error("JWT 필터 처리 중 오류 발생: {}", e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"인증 처리 중 오류가 발생했습니다\",\"code\":\"AUTH_ERROR\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}