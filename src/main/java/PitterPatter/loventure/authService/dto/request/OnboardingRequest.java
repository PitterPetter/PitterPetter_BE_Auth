package PitterPatter.loventure.authService.dto.request;

import java.time.LocalDate;
import java.util.List;

import PitterPatter.loventure.authService.repository.DateCostPreference;
import PitterPatter.loventure.authService.repository.FavoriteFoodCategories;
import PitterPatter.loventure.authService.repository.Gender;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OnboardingRequest {

    @Size(max = 50, message = "닉네임은 50자 이하여야 합니다")
    private String nickname;

    private LocalDate birthDate;

    private Gender gender;

    @Min(value = 1, message = "알코올 선호도는 1 이상이어야 합니다")
    @Max(value = 5, message = "알코올 선호도는 5 이하여야 합니다")
    private Integer alcoholPreference;
    
    @Min(value = 1, message = "활동 범위는 1 이상이어야 합니다")
    @Max(value = 5, message = "활동 범위는 5 이하여야 합니다")
    private Integer activeBound;
    
    private List<FavoriteFoodCategories> favoriteFoodCategories;
    private DateCostPreference dateCostPreference;
    private String preferredAtmosphere;
}