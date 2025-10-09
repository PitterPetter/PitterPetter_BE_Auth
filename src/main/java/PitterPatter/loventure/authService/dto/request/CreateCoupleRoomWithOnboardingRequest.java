package PitterPatter.loventure.authService.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 커플룸 생성과 온보딩을 함께 처리하는 Request DTO
 */
public record CreateCoupleRoomWithOnboardingRequest(
    @NotBlank(message = "커플홈 이름은 필수입니다")
    @Size(max = 50, message = "커플홈 이름은 50자를 초과할 수 없습니다")
    String coupleHomeName,
    
    @NotNull(message = "데이트 시작일은 필수입니다")
    String datingStartDate
) {
}
