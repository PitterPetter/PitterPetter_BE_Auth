package PitterPatter.loventure.authService.exception;

public enum ErrorCode {
    // 4xx Client Errors
    VALIDATION_ERROR("40001", "유효성 검사 실패"),
    INVALID_USER_ID_FORMAT("40004", "잘못된 사용자 ID 형식입니다"),
    ALREADY_COUPLED("40005", "이미 커플 상태입니다"),
    
    // 401 Unauthorized
    UNAUTHORIZED("40101", "인증이 필요합니다"),
    NO_PERMISSION("40102", "권한이 없습니다"),
    
    // 404 Not Found
    USER_NOT_FOUND("40401", "존재하지 않는 회원입니다"),
    USER_NOT_FOUND_BY_ID("40402", "존재하지 않는 회원(userId)입니다"),
    INVITE_CODE_NOT_FOUND("40403", "초대 코드가 존재하지 않습니다"),
    COUPLE_NOT_FOUND("40404", "존재하지 않는 커플입니다"),
    
    // 409 Conflict
    USER_ALREADY_EXISTS("40901", "이미 존재하는 회원입니다"),
    ALREADY_MATCHED_CODE("40902", "이미 매칭된 초대 코드입니다"),
    ALREADY_CANCELLED("40903", "이미 취소된 상태입니다"),
    
    // 5xx Server Errors
    INTERNAL_SERVER_ERROR("50001", "서버 내부 오류가 발생했습니다"),
    RECOMMENDATION_DATA_ERROR("50002", "추천 데이터 조회 중 오류가 발생했습니다"),
    REFRESH_TOKEN_ERROR("50003", "토큰 갱신 중 오류가 발생했습니다"),
    REDIRECT_URL_ERROR("50004", "사용자 리다이렉트 URL 조회 중 오류가 발생했습니다"),
    USER_INFO_ERROR("50005", "사용자 정보 조회 중 오류가 발생했습니다"),
    ACCOUNT_STATUS_ERROR("50006", "계정 상태 확인 중 오류가 발생했습니다"),
    LOGOUT_ERROR("50007", "로그아웃 중 오류가 발생했습니다");
    
    private final String code;
    private final String message;
    
    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getMessage() {
        return message;
    }
}
