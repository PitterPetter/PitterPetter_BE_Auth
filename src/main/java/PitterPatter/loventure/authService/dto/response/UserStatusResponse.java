package PitterPatter.loventure.authService.dto.response;

public record UserStatusResponse(
    boolean success,
    String redirectUrl,
    String status,
    boolean isOnboardingCompleted,
    boolean isCoupled,
    boolean isRockCompleted
) {}
