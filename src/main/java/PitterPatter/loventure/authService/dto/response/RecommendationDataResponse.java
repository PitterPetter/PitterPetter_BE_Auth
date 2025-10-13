package PitterPatter.loventure.authService.dto.response;

public record RecommendationDataResponse(
    RecommendationUserResponse user,
    RecommendationUserResponse partner,
    RecommendationCoupleResponse couple
) {
}