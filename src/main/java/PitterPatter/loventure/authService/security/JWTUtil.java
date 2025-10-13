package PitterPatter.loventure.authService.security;

import java.security.Key;
import java.util.Base64; // Base64 import 추가
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class JWTUtil {
    // 실제 JWT 생성하고 검증하는 class
    private final Key key;

    // JWTUtil class를 생성해서 application.yml 파일에 존재하는 키를 가져와 인코딩
    // Base64 디코딩 로직 추가 (Gateway와 통일)
    public JWTUtil(@Value("${spring.jwt.secret}") String secret) {
        Key tempKey;
        try {
            // Base64 인코딩된 값 -> 바이트 배열로 디코딩 시도
            byte[] decodedKey = Base64.getDecoder().decode(secret);
            tempKey = Keys.hmacShaKeyFor(decodedKey);
            log.info("JWT 시크릿 키를 Base64 디코딩하여 설정했습니다. 길이: {}", decodedKey.length);
        } catch (IllegalArgumentException e) {
            // Base64 디코딩 실패 시 원본 문자열을 UTF-8 바이트로 변환
            tempKey = Keys.hmacShaKeyFor(secret.getBytes());
            log.info("JWT 시크릿 키를 UTF-8 바이트로 변환하여 설정했습니다. 길이: {}", secret.getBytes().length);
        }
        this.key = tempKey;
    }

    // userID를 포함한 JWT access token 생성
    public String createJwtWithUserId(String username, String userId, Long expiredMs) {
        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiredMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // userId와 coupleId를 포함한 JWT access token 생성
    public String createJwtWithUserIdAndCoupleId(String username, String userId, String coupleId, Long expiredMs) {
        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
                .claim("coupleId", coupleId)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiredMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // Refresh token 생성 -> access token을 계속 사용하는 것은 보안상 좋지 않음
    // 따라서 만료 기간을 두고 토큰이 만료 시 refresh token으로 새롭게 발급
    public String createRefreshToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 14 * 24 * 60 * 60 * 1000L)) // 14일 (2주)
                .claim("type", "refresh")
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // 인코딩된 token을 디코딩 하고 저장된 사용자 아이디 반환
    public String getUsername(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().getSubject();
    }

    // 설정된 token 만료 시간을 현재 시간과 비교해 유효성 검사 진행
    public boolean isTokenExpired(String token) {
        try {
            return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().getExpiration().before(new Date());
        } catch (Exception e) {
            // 토큰 파싱 실패 시 만료된 것으로 간주
            return true;
        }
    }

    // JWT에서 userID 추출 (String 기반)
    public String getUserId(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().get("userId", String.class);
    }

    // JWT에서 coupleId 추출
    public String getCoupleIdFromToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().get("coupleId", String.class);
    }
}