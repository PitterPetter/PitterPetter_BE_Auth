// SecurityConfig.java (수정 후: OAuth2 State 관리 및 import 오류 수정)

package PitterPatter.loventure.authService.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import PitterPatter.loventure.authService.handler.OAuth2LoginFailureHandler;
import PitterPatter.loventure.authService.handler.OAuth2LoginSuccessHandler;
import PitterPatter.loventure.authService.repository.UserRepository;
import PitterPatter.loventure.authService.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    // 서비스의 보안 규칙을 담당
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final OAuth2LoginFailureHandler oAuth2LoginFailureHandler;
    private final JWTUtil jwtUtil;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final UserRepository userRepository;

    // [수정] OAuth2 인증 요청을 쿠키에 저장하는 리포지토리 (STATELESS 환경에 적합)
    @Bean
    public CookieOAuth2AuthorizationRequestRepository authorizationRequestRepository() {
        return new CookieOAuth2AuthorizationRequestRepository();
    }

    // CORS 설정 (AI 서비스와의 연동을 위해)
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 허용할 Origin 설정
        configuration.addAllowedOriginPattern("*"); // 개발 환경에서는 모든 Origin 허용
        configuration.addAllowedMethod("*"); // 모든 HTTP 메서드 허용
        configuration.addAllowedHeader("*"); // 모든 헤더 허용
        configuration.setAllowCredentials(true); // 쿠키 허용
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        // CSRF, Form Login, HTTP Basic 비활성화
        // 세션이나 쿠키 기반의 보호방식이나 전통적인 로그인 폼 인증은 JWT에서 사용하지 않기 때문에 비활성 처리
        http
                .csrf((csrf) -> csrf.disable())
                .formLogin((formLogin) -> formLogin.disable())
                .httpBasic((httpBasic) -> httpBasic.disable())
                .cors((cors) -> cors.configurationSource(corsConfigurationSource())); // CORS 설정 적용

        // 세션 관리 정책: STATELESS (JWT 사용을 위함)
        // 세션을 통해 사용자의 로그인 상태를 기억하면 보안적인 문제가 생김
        // JWT를 이용해 refresh 등의 작업을 이용해서 처리하기 위해 로그인 상태를 기억하지 않게 STATELESS 처리

        http
                .sessionManagement((session) -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // OAuth2 로그인 설정
        http
                .oauth2Login((oauth2) -> oauth2
                        .successHandler(oAuth2LoginSuccessHandler) // 로그인 성공 핸들러로 이동 -> 유저를 DB에 저장하거나 조회 후 실행, JWT 발급
                        .failureHandler(oAuth2LoginFailureHandler) // 로그인 실패 핸들러로 이동
                        .authorizationEndpoint(authorizationEndpointConfig -> authorizationEndpointConfig
                                .authorizationRequestRepository(authorizationRequestRepository()))
                        .userInfoEndpoint(userInfoEndpointConfig -> userInfoEndpointConfig
                                .userService(customOAuth2UserService))); // 실제 유저 정보 처리 service

        // JWT Filter -> Spring Security의 필터 체인에 추가
        http
                .addFilterBefore(new JWTFilter(jwtUtil, userRepository), UsernamePasswordAuthenticationFilter.class);

        http
                .authorizeHttpRequests((auth) -> auth
                        .requestMatchers("/", "/login/**", "/oauth2/**", "/api/test/**", "/api/auth/status",
                                "/api/auth/signup", "/api/auth/login/**",
                                "/api/auth/swagger-ui/**", "/api/auth/v3/api-docs/**", "/api/auth/swagger-ui.html",
                                "/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html", "/actuator/**").permitAll() // 누구나 접근 가능한 경로
                        .requestMatchers("/api/users/recommendation-data/**").permitAll() // AI 서비스에서 호출하는 API 허용
                        .anyRequest().authenticated()); // 나머지 경로는 인증 필요

        return http.build();
    }
}