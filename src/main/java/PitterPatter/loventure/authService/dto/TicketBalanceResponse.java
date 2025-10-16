package PitterPatter.loventure.authService.dto;

import java.time.LocalDateTime;

/**
 * 티켓 잔액 응답 DTO
 */
public record TicketBalanceResponse(
    String coupleId,
    Integer ticket,
    Boolean isTodayTicket,
    LocalDateTime lastSyncedAt
) {
}
