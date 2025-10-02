package PitterPatter.loventure.authService.dto.response;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import PitterPatter.loventure.authService.repository.CoupleOnboarding;
import PitterPatter.loventure.authService.repository.Gender;
import PitterPatter.loventure.authService.repository.TodayCondition;
import PitterPatter.loventure.authService.repository.User;
import lombok.Builder; // [New] Optional import
import lombok.Getter;

@Getter
@Builder
public class RecommendationDataResponse {
    private UserData userData;
    private CoupleData coupleData; // 커플이 아닐 경우 null

    // User 엔티티에서 UserData DTO로 변환하는 팩토리 메서드
    public static UserData fromUserEntity(User user) {
        return UserData.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .name(user.getName())
                .birthDate(user.getBirthDate())
                // Enum을 String으로 변환 (수정: Optional을 사용하여 null-safe하게 toString() 호출)
                .gender(Optional.ofNullable(user.getGender())
                        .map(Gender::toString)
                        .orElse(null))
                .alcoholPreference(user.getAlcoholPreference())
                .activeBound(user.getActiveBound())
                // List<Enum>을 List<String>으로 변환 (toString() 사용)
                .favoriteFoodCategories(user.getFavoriteFoodCategories().stream()
                        .map(e -> e.toString())
                        .collect(Collectors.toList()))
                // Enum을 String으로 변환 (수정: Optional을 사용하여 null-safe하게 toString() 호출)
                .dateCostPreference(Optional.ofNullable(user.getDateCostPreference())
                        .map(e -> e.toString())
                        .orElse(null))
                .preferredAtmosphere(user.getPreferredAtmosphere())
                .build();
    }

    // CoupleOnboarding 엔티티에서 CoupleData DTO로 변환하는 팩토리 메서드
    public static CoupleData fromCoupleOnboardingAndId(CoupleOnboarding coupleOnboarding, String coupleId) {
        return CoupleData.builder()
                .coupleId(coupleId)
                .todayCondition(coupleOnboarding.getTodayCondition())
                .drinking(coupleOnboarding.getDrinking())
                .hateFood(coupleOnboarding.getHateFood())
                .build();
    }

    @Getter
    @Builder
    public static class UserData {
        private BigInteger userId;
        private String email;
        private String name;
        private LocalDateTime birthDate;
        private String gender;
        private Integer alcoholPreference;
        private Integer activeBound;
        private List<String> favoriteFoodCategories;
        private String dateCostPreference;
        private String preferredAtmosphere;
    }

    @Getter
    @Builder
    public static class CoupleData {
        private String coupleId;
        private TodayCondition todayCondition;
        private String drinking;
        private String hateFood;
    }
}