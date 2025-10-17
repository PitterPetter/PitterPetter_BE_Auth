package PitterPatter.loventure.authService.dto;

import java.time.OffsetDateTime;

/**
 * 티켓 정보 DTO
 * Gateway에서 사용하는 티켓 정보 응답
 */
public record TicketInfo(
    String coupleId,
    int ticket,
    boolean isTodayTicket,
    OffsetDateTime lastSyncedAt
) {}
