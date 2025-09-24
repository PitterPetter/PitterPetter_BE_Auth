package PitterPatter.loventure.authService.dto.request;

import java.util.List;

import PitterPatter.loventure.authService.repository.DateCostPreference;
import PitterPatter.loventure.authService.repository.FavoriteFoodCategories;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OnboardingRequest {

    @Min(value = 0, message = "알코올 선호도는 0 이상이어야 합니다")
    @Max(value = 10, message = "알코올 선호도는 10 이하여야 합니다")
    private Integer alcoholPreference;
    
    @Min(value = 0, message = "활동 범위는 0 이상이어야 합니다")
    @Max(value = 10, message = "활동 범위는 10 이하여야 합니다")
    private Integer activeBound;
    
    private List<FavoriteFoodCategories> favoriteFoodCategories;
    private DateCostPreference dateCostPreference;
    private String preferredAtmosphere;
}