package PitterPatter.loventure.authService.dto.response;

import java.util.List;

public record RecommendationDataResponse(
    UserData user,
    CoupleData couple
) {
    public record UserData(
        String userId,
        String name,
        String alcoholPreference,
        String activeBound,
        List<String> favoriteFoodCategories,
        String dateCostPreference,
        String preferredAtmosphere
    ) {}
    
    public record CoupleData(
        String coupleId,
        String coupleHomeName,
        String datingStartDate
    ) {}
}
