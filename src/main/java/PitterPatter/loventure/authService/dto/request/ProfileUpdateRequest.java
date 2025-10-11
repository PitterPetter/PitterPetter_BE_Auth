package PitterPatter.loventure.authService.dto.request;

import java.time.LocalDateTime;
import java.util.List;

import PitterPatter.loventure.authService.repository.DateCostPreference;
import PitterPatter.loventure.authService.repository.FavoriteFoodCategories;
import PitterPatter.loventure.authService.repository.Gender;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
    @Size(max = 100, message = "이름은 100자 이하여야 합니다")
    String name,
    
    @Size(max = 50, message = "닉네임은 50자 이하여야 합니다")
    String nickname,
    
    LocalDateTime birthDate,
    
    Gender gender,
    
    @Min(value = 1, message = "알코올 선호도는 1 이상이어야 합니다")
    @Max(value = 5, message = "알코올 선호도는 5 이하여야 합니다")
    Integer alcoholPreference,
    
    @Min(value = 1, message = "활동 범위는 1 이상이어야 합니다")
    @Max(value = 5, message = "활동 범위는 5 이하여야 합니다")
    Integer activeBound,
    
    List<FavoriteFoodCategories> favoriteFoodCategories,
    
    DateCostPreference dateCostPreference,
    
    @Size(max = 500, message = "선호 분위기는 500자 이하여야 합니다")
    String preferredAtmosphere
) {}
