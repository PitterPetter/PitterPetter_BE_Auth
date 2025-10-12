package PitterPatter.loventure.authService.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CoupleMatchRequest(
    @NotBlank(message = "초대 코드는 필수입니다")
    @Pattern(regexp = "^[A-Z0-9]{6}$", message = "초대 코드는 6자리 영문 대문자와 숫자 조합이어야 합니다")
    String inviteCode
) {
    @JsonCreator
    public static CoupleMatchRequest fromString(String inviteCode) {
        return new CoupleMatchRequest(inviteCode);
    }
    
    @JsonCreator
    public static CoupleMatchRequest fromJson(@JsonProperty("inviteCode") String inviteCode) {
        return new CoupleMatchRequest(inviteCode);
    }
}