package PitterPatter.loventure.authService.dto.response;

import jakarta.validation.constraints.NotBlank;

public record OnboardingResponse(
    @NotBlank(message = "사용자 ID는 필수입니다")
    String userId,
    
    boolean onboardingCompleted
) {}
