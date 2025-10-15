package PitterPatter.loventure.authService.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Gateway 연동용 티켓 API 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Gateway 연동용 티켓 API 응답")
public class TicketResponse {

    @Schema(description = "응답 상태", example = "success")
    private String status;

    @Schema(description = "티켓 정보")
    private TicketInfo data;
}
