package PitterPatter.loventure.authService.dto;

import java.time.LocalDateTime;

/**
 * 티켓 정보 DTO
 */
public record TicketInfo(
    String coupleId,
    Integer ticket,
    Boolean isTodayTicket,
    LocalDateTime lastSyncedAt
) {
}
