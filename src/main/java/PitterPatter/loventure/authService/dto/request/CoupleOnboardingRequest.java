package PitterPatter.loventure.authService.dto.request;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CoupleOnboardingRequest(
    @NotBlank(message = "커플 홈 이름은 필수입니다")
    String coupleHomeName,

    @NotNull(message = "사귄 날짜는 필수입니다")
    LocalDateTime datingStartDate
) {}

