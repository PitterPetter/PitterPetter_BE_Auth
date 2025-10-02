package PitterPatter.loventure.authService.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoupleMatchResponse {
    private String coupleId;
    private String creatorUserId;
    private String partnerUserId;
}

