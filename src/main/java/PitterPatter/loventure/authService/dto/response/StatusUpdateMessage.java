package PitterPatter.loventure.authService.dto.response;

import java.time.LocalDateTime;

public record StatusUpdateMessage(
    String status,
    String redirectUrl,
    boolean isRockCompleted,
    LocalDateTime timestamp
) {}
