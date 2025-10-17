package PitterPatter.loventure.authService.dto.response;

import PitterPatter.loventure.authService.repository.AccountStatus;
import PitterPatter.loventure.authService.repository.ProviderType;

public record UserExistsResponse(
    boolean exists,
    AccountStatus status,
    ProviderType providerType
) {}
