package PitterPatter.loventure.authService.dto.request;

import jakarta.validation.constraints.Size;

/**
 * 커플 정보 변경을 위한 Request DTO
 */
public record CoupleUpdateRequest(
    @Size(max = 50, message = "커플홈 이름은 50자를 초과할 수 없습니다")
    String coupleHomeName,
    
    String datingStartDate
) {}

