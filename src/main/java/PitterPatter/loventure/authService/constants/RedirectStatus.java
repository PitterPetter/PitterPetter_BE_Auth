package PitterPatter.loventure.authService.constants;

/**
 * 리다이렉트 상태 상수 정의
 * 사용자 상태별 리다이렉트 처리를 위한 표준화된 상태 값
 */
public final class RedirectStatus {
    
    // 사용자 상태별 리다이렉트 상태
    public static final String ONBOARDING_REQUIRED = "ONBOARDING_REQUIRED";
    public static final String COUPLE_MATCHING_REQUIRED = "COUPLE_MATCHING_REQUIRED";
    public static final String COMPLETED = "COMPLETED";
    
    // 에러 상태
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String USER_NOT_FOUND = "USER_NOT_FOUND";
    public static final String REDIRECT_URL_ERROR = "REDIRECT_URL_ERROR";
    
    private RedirectStatus() {
        // 유틸리티 클래스는 인스턴스화 방지
    }
}
