package PitterPatter.loventure.authService.dto.response;

public record RecommendationCoupleResponse(
    Long id,
    Long boyfriendId,
    Long girlfriendId,
    String name,
    Integer reroll,
    Integer ticket,
    Integer loveDay,
    Integer diaryCount
) {}
