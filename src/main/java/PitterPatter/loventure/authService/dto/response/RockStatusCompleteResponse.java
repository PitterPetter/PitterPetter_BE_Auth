package PitterPatter.loventure.authService.dto.response;

public record RockStatusCompleteResponse(
    boolean success,
    String message,
    String coupleId,
    String userId
) {}
