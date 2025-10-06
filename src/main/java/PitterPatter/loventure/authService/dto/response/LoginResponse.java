package PitterPatter.loventure.authService.dto.response;

public record LoginResponse(
    String user_id,
    boolean isNewUser,
    String email,
    String name,
    String JWT,
    String refresh_token
) {}

