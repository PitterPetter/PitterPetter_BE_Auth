package PitterPatter.loventure.authService.dto.response;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateCoupleRoomResponse(
    @NotBlank(message = "초대 코드는 필수입니다")
    @Pattern(regexp = "^[A-Z0-9]{6}$", message = "초대 코드는 6자리 영문 대문자와 숫자 조합이어야 합니다")
    String inviteCode
) {}

