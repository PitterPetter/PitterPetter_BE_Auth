package PitterPatter.loventure.authService.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 티켓 추가 응답 (코스 저장 시)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "티켓 추가 결과")
public class TicketEarnResponse {

    @Schema(description = "추가 성공 여부 (오늘 이미 사용했으면 false)", example = "true")
    private Boolean earned;

    @Schema(description = "새로운 티켓 잔액", example = "4")
    private Integer newBalance;

    @Schema(description = "응답 메시지", example = "티켓이 추가되었습니다.")
    private String message;
}
