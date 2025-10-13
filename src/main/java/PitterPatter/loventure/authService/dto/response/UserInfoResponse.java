package PitterPatter.loventure.authService.dto.response;

/**
 * 유저 정보 조회 API 응답 DTO
 * 다른 서비스에서 유저 이름을 조회할 때 사용
 */
public record UserInfoResponse(
        String userId,    // userId (BigInteger를 String으로 변환)
        String name       // 사용자 이름
) {}

