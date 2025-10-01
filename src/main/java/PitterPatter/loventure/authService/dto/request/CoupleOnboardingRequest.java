package PitterPatter.loventure.authService.dto.request;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CoupleOnboardingRequest {

    @NotBlank(message = "커플 홈 이름은 필수입니다")
    private String coupleHomeName;

    @NotNull(message = "사귄 날짜는 필수입니다")
    private LocalDateTime datingStartDate;
}

