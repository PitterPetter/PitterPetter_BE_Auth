package PitterPatter.loventure.authService.dto.response;

public record RecommendationCoupleResponse(
    String id,
    String boyfriendId,
    String girlfriendId,
    String name,
    Integer reroll,
    Integer ticket,
    Integer loveDay,
    Integer diaryCount
) {}
