package PitterPatter.loventure.authService.constants;

/**
 * 에러 코드 상수 정의
 * 중복된 에러 코드 문자열을 상수로 관리하여 일관성 확보
 */
public final class ErrorConstants {
    
    // 4xx Client Errors
    public static final String VALIDATION_ERROR = "40001";
    public static final String INVALID_USER_ID_FORMAT = "40004";
    public static final String ALREADY_COUPLED = "40005";
    
    // 401 Unauthorized
    public static final String UNAUTHORIZED = "40101";
    public static final String NO_PERMISSION = "40102";
    
    // 404 Not Found
    public static final String USER_NOT_FOUND = "40401";
    public static final String USER_NOT_FOUND_BY_ID = "40402";
    public static final String INVITE_CODE_NOT_FOUND = "40403";
    public static final String COUPLE_NOT_FOUND = "40404";
    
    // 409 Conflict
    public static final String USER_ALREADY_EXISTS = "40901";
    public static final String ALREADY_MATCHED_CODE = "40902";
    public static final String ALREADY_CANCELLED = "40903";
    
    // 5xx Server Errors
    public static final String INTERNAL_SERVER_ERROR = "50001";
    public static final String RECOMMENDATION_DATA_ERROR = "50002";
    public static final String REFRESH_TOKEN_ERROR = "50003";
    public static final String REDIRECT_URL_ERROR = "50004";
    public static final String USER_INFO_ERROR = "50005";
    public static final String ACCOUNT_STATUS_ERROR = "50006";
    public static final String LOGOUT_ERROR = "50007";
    
    private ErrorConstants() {
        // 유틸리티 클래스는 인스턴스화 방지
    }
}
