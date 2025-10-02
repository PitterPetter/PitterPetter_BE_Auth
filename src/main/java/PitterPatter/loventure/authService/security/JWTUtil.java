package PitterPatter.loventure.authService.security;

import java.math.BigInteger;
import java.security.Key;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component
public class JWTUtil {
    // 실제 JWT 생성하고 검증하는 class
    private final Key key;

    // JWTUtil class를 생성해서 application.yml 파일에 존재하는 키를 가져와 인코딩
    public JWTUtil(@Value("${spring.jwt.secret}") String secret) {
        // 일반 문자열을 바이트 배열로 변환 (BASE64 디코딩 제거)
        byte[] byteSecretKey = secret.getBytes();
        this.key = Keys.hmacShaKeyFor(byteSecretKey);
    }

    // 사용자에게 부여할 JWT access token 생성
    public String createJwt(String username, Long expiredMs) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiredMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
    
    // userID를 포함한 JWT access token 생성
    public String createJwtWithUserId(String username, BigInteger userId, Long expiredMs) {
        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId.toString())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiredMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
    
    // userId와 coupleId를 포함한 JWT access token 생성
    public String createJwtWithUserIdAndCoupleId(String username, BigInteger userId, String coupleId, Long expiredMs) {
        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId.toString())
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
                .setExpiration(new Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L)) // 7일
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
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().getExpiration().before(new Date());
    }
    
    // JWT에서 userID 추출 (BigInteger 기반)
    public BigInteger getUserId(String token) {
        String userIdStr = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().get("userId", String.class);
        return new BigInteger(userIdStr);
    }
    
    // JWT에서 coupleId 추출
    public String getCoupleIdFromToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().get("coupleId", String.class);
    }
}