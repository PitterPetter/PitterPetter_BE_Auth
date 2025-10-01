package PitterPatter.loventure.authService.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CoupleMatchRequest {
    private String userId;
    private String inviteCode;
}