package PitterPatter.loventure.authService.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String user_id;
    private boolean isNewUser;
    private String email;
    private String name;
    private String JWT;
    private String refresh_token;
}

