package PitterPatter.loventure.authService.dto.response;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;

import PitterPatter.loventure.authService.repository.AccountStatus;
import PitterPatter.loventure.authService.repository.DateCostPreference;
import PitterPatter.loventure.authService.repository.FavoriteFoodCategories;
import PitterPatter.loventure.authService.repository.Gender;
import PitterPatter.loventure.authService.repository.ProviderType;

public record MyPageResponse(
    // 기본 정보
    BigInteger userId,
    String email,
    String name,
    String nickname,
    ProviderType providerType,
    String providerId,
    AccountStatus status,
    
    // 개인 정보
    LocalDateTime birthDate,
    Gender gender,
    
    // 온보딩 정보
    Integer alcoholPreference,
    Integer activeBound,
    List<FavoriteFoodCategories> favoriteFoodCategories,
    DateCostPreference dateCostPreference,
    String preferredAtmosphere,
    
    // 계정 정보
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    Integer rerollCount,
    
    // 커플 정보
    CoupleInfo coupleInfo
) {
    
    public record CoupleInfo(
        String coupleId,
        String coupleHomeName,
        LocalDateTime datingStartDate,
        String partnerName,
        String partnerEmail,
        String partnerProviderId,
        String status,
        LocalDateTime coupleCreatedAt
    ) {}
}
