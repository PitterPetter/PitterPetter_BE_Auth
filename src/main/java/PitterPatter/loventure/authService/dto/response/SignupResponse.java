package PitterPatter.loventure.authService.dto.response;

import java.time.LocalDateTime;

public record SignupResponse(
    String userId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}

