package PitterPatter.loventure.authService.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import PitterPatter.loventure.authService.repository.CoupleOnboarding;
import PitterPatter.loventure.authService.repository.CoupleRoom;
import PitterPatter.loventure.authService.repository.FavoriteFoodCategories;
import PitterPatter.loventure.authService.repository.User;

/**
 * AI 서비스에서 사용할 사용자 및 커플 데이터 응답 DTO
 * record 기반으로 변경하여 불변성과 간결성 확보
 */
public record RecommendationDataResponse(
        UserData userData,
        CoupleData coupleData
) {
    
    /**
     * 사용자 데이터 record - feature/PIT-328의 상세한 사용자 정보 포함
     */
    public record UserData(
            String userId,
            String email,
            String name,
            String nickname,
            LocalDate birthDate,
            String gender,
            Integer alcoholPreference,
            Integer activeBound,
            List<FavoriteFoodCategories> favoriteFoodCategories,
            String dateCostPreference,
            String preferredAtmosphere,
            String providerType,
            String providerId,
            String status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}
    
    /**
     * 커플 데이터 record - CoupleOnboarding 정보 포함
     */
    public record CoupleData(
            String coupleId,
            String coupleHomeName,
            String creatorUserId,
            String partnerUserId,
            LocalDate datingStartDate,
            String status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            CoupleOnboardingData onboardingData
    ) {}
    
    /**
     * 커플 온보딩 데이터 record - feature/PIT-328의 상세한 온보딩 정보
     */
    public record CoupleOnboardingData(
            String onboardingId,
            String todayCondition,
            String drinking,
            String hateFood,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}
    
    /**
     * User 엔티티와 CoupleRoom 엔티티로부터 RecommendationDataResponse 생성
     * 팩토리 메서드 패턴 적용 - feature/PIT-328의 상세한 정보 포함
     */
    public static RecommendationDataResponse from(User user, CoupleRoom coupleRoom, CoupleOnboarding coupleOnboarding) {
        UserData userData = new UserData(
                user.getUserId().toString(),
                user.getEmail(),
                user.getName(),
                user.getNickname(),
                user.getBirthDate(),
                user.getGender() != null ? user.getGender().name() : null,
                user.getAlcoholPreference(),
                user.getActiveBound(),
                user.getFavoriteFoodCategories(),
                user.getDateCostPreference() != null ? user.getDateCostPreference().name() : null,
                user.getPreferredAtmosphere(),
                user.getProviderType().name(),
                user.getProviderId(),
                user.getStatus().name(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
        
        CoupleData coupleData = coupleRoom != null ? new CoupleData(
                coupleRoom.getCoupleId(),
                coupleRoom.getCoupleHomeName(),
                coupleRoom.getCreatorUserId(),
                coupleRoom.getPartnerUserId(),
                coupleRoom.getDatingStartDate(),
                coupleRoom.getStatus().name(),
                coupleRoom.getCreatedAt(),
                coupleRoom.getUpdatedAt(),
                coupleOnboarding != null ? new CoupleOnboardingData(
                        coupleOnboarding.getOnboardingId(),
                        coupleOnboarding.getTodayCondition() != null ? coupleOnboarding.getTodayCondition().name() : null,
                        coupleOnboarding.getDrinking(),
                        coupleOnboarding.getHateFood(),
                        coupleOnboarding.getCreatedAt(),
                        coupleOnboarding.getUpdatedAt()
                ) : null
        ) : null;
        
        return new RecommendationDataResponse(userData, coupleData);
    }
    
    /**
     * User 엔티티만으로 RecommendationDataResponse 생성 (커플이 없는 경우)
     */
    public static RecommendationDataResponse from(User user) {
        return from(user, null, null);
    }
    
    /**
     * User 엔티티와 CoupleRoom 엔티티로부터 RecommendationDataResponse 생성 (CoupleOnboarding 없이)
     */
    public static RecommendationDataResponse from(User user, CoupleRoom coupleRoom) {
        return from(user, coupleRoom, null);
    }
}