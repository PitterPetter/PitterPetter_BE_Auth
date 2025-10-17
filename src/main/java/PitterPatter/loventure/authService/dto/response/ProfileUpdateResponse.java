package PitterPatter.loventure.authService.dto.response;

public record ProfileUpdateResponse(
    boolean success,
    String message,
    String userId
) {}
