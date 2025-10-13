package PitterPatter.loventure.authService.dto.response;

import java.time.LocalDateTime;

public record RecommendationUserResponse(
    Long id,
    String name,
    String birthday,
    String gender,
    Boolean likeAlcohol,
    Boolean active,
    String favoriteFood,
    Integer avgDateCost,
    String preferredVibe,
    String uuid,
    String status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
