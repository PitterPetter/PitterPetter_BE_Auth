package PitterPatter.loventure.authService.dto.response;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CoupleTicketResponse(
    @NotNull(message = "커플 ID는 필수입니다")
    String coupleId,
    
    @PositiveOrZero(message = "티켓 수는 0 이상이어야 합니다")
    Integer ticket,
    
    @NotNull(message = "마지막 동기화 시간은 필수입니다")
    String lastSyncedAt
) {}
