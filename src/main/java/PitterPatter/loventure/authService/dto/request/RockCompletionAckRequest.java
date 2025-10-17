package PitterPatter.loventure.authService.dto.request;

import java.time.LocalDateTime;

public record RockCompletionAckRequest(
    String coupleId,
    String userId,
    LocalDateTime completedAt
) {}
