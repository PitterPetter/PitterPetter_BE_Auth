package PitterPatter.loventure.authService.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Gateway 연동용 티켓 API 응답 DTO
 */
@Schema(description = "Gateway 연동용 티켓 API 응답")
public record TicketResponse(
    @Schema(description = "응답 상태", example = "success")
    String status,

    @Schema(description = "티켓 정보")
    TicketInfo data
) {
}
