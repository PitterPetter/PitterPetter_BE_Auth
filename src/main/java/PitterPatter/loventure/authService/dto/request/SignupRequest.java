package PitterPatter.loventure.authService.dto.request;

import PitterPatter.loventure.authService.repository.ProviderType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignupRequest {
    private String email;
    private String name;
    private String providerId;
    private ProviderType providerType;
}

