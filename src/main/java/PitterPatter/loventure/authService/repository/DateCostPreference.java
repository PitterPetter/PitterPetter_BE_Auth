package PitterPatter.loventure.authService.repository;

public enum DateCostPreference {
    만원_미만("만원 미만"),
    만원_3만원("만원 3만원"),
    삼만원_5만원("삼만원 5만원"),
    오만원_8만원("오만원 8만원"),
    팔만원_이상("팔만원 이상");
    
    private final String displayName;
    
    DateCostPreference(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
