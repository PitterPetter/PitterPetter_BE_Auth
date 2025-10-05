package PitterPatter.loventure.authService.dto.response;

public record AuthResponse(
    boolean success,
    String message,
    String accessToken,
    String refreshToken,
    Long expiresIn,
    UserInfo user
) {
    public record UserInfo(
        String userId,
        String email,
        String name,
        String providerType,
        String providerId,
        String status,
        boolean isNewUser // 신규 가입 여부
    ) {}
}
