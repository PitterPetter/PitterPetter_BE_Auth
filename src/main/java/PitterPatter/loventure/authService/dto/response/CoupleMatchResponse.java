package PitterPatter.loventure.authService.dto.response;

import jakarta.validation.constraints.NotBlank;

public record CoupleMatchResponse(
    @NotBlank(message = "커플 ID는 필수입니다")
    String coupleId,
    
    @NotBlank(message = "생성자 사용자 ID는 필수입니다")
    String creatorUserId,
    
    @NotBlank(message = "파트너 사용자 ID는 필수입니다")
    String partnerUserId,
    
    // 새 JWT 토큰 (coupleId 포함)
    String accessToken
) {}

