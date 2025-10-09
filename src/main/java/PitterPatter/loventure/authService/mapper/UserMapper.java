package PitterPatter.loventure.authService.mapper;

import org.springframework.stereotype.Component;

import PitterPatter.loventure.authService.dto.response.AuthResponse;
import PitterPatter.loventure.authService.dto.response.DeleteUserResponse;
import PitterPatter.loventure.authService.dto.response.SignupResponse;
import PitterPatter.loventure.authService.repository.User;

/**
 * User 관련 Entity와 DTO 간의 변환을 담당하는 Mapper
 */
@Component
public class UserMapper {
    
    /**
     * User를 AuthResponse.UserInfo로 변환
     */
    public AuthResponse.UserInfo toUserInfo(User user, boolean isNewUser) {
        return new AuthResponse.UserInfo(
            user.getUserId().toString(),
            user.getEmail(),
            user.getName(),
            user.getProviderType().name(),
            user.getProviderId(),
            user.getStatus().name(),
            isNewUser
        );
    }
    
    
    /**
     * User를 SignupResponse로 변환
     */
    public SignupResponse toSignupResponse(User user) {
        return new SignupResponse(
            user.getUserId().toString(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }
    
    /**
     * 삭제 성공 응답 생성
     */
    public DeleteUserResponse toDeleteUserResponse() {
        return new DeleteUserResponse("success", "회원 탈퇴 완료");
    }
}

