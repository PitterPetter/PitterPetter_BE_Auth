package PitterPatter.loventure.authService.dto.response;

import jakarta.validation.constraints.NotBlank;

public record DeleteUserResponse(
    @NotBlank(message = "상태는 필수입니다")
    String status,
    
    @NotBlank(message = "메시지는 필수입니다")
    String message
) {}

