package PitterPatter.loventure.authService.repository;

import com.fasterxml.jackson.annotation.JsonAlias;

public enum DateCostPreference {
    @JsonAlias({"만원미만", "만원_미만"})
    만원_미만("만원 미만"),
    
    @JsonAlias({"만원삼만원", "만원_삼만원"})
    만원_삼만원("만원 삼만원"),
    
    @JsonAlias({"삼만원오만원", "삼만원_오만원"})
    삼만원_오만원("삼만원 오만원"),
    
    @JsonAlias({"오만원팔만원", "오만원_팔만원"})
    오만원_팔만원("오만원 팔만원"),
    
    @JsonAlias({"팔만원이상", "팔만원_이상"})
    팔만원_이상("팔만원 이상");
    
    private final String displayName;
    
    DateCostPreference(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
