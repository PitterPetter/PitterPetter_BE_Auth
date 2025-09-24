package PitterPatter.loventure.authService.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignupResponse {
    private String userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

