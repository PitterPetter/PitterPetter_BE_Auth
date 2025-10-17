package PitterPatter.loventure.authService.dto.request;

public record RockStatusCompleteRequest(
    String coupleId,
    String userId,
    String territoryId
) {}
