package PitterPatter.loventure.authService.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 티켓 사용 응답 (지역락 해제 시)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "티켓 사용 결과")
public class TicketUseResponse {

    @Schema(description = "사용 성공 여부", example = "true")
    private Boolean success;

    @Schema(description = "남은 티켓 수", example = "2")
    private Integer remainingBalance;

    @Schema(description = "응답 메시지", example = "티켓이 사용되었습니다.")
    private String message;
}
