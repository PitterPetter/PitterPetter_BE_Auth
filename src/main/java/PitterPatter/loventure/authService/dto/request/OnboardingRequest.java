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

    // 온보딩에서는 닉네임, 생일, 성별을 받지 않음
    // 이 값들은 provider로부터 제공받지 못하면 기본값 null로 두고 mypage에서 수정

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