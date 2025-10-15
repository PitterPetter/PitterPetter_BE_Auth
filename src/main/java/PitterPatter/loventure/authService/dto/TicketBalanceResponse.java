package PitterPatter.loventure.authService.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 티켓 잔액 조회 응답
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "티켓 잔액 정보")
public class TicketBalanceResponse {

    @Schema(description = "커플 ID", example = "0N851ZN4CGWP8")
    private String coupleId;

    @Schema(description = "티켓 잔액", example = "3")
    private Integer ticket;

    @Schema(description = "오늘 티켓 사용 가능 여부", example = "true")
    private Boolean isTodayTicket;

    @Schema(description = "마지막 동기화 시간", example = "2024-12-19T10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastSyncedAt;
}