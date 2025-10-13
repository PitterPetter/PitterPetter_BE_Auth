package PitterPatter.loventure.authService.mapper;

import org.springframework.stereotype.Component;

import PitterPatter.loventure.authService.dto.response.CoupleMatchResponse;
import PitterPatter.loventure.authService.dto.response.CreateCoupleRoomResponse;
import PitterPatter.loventure.authService.repository.CoupleRoom;
import PitterPatter.loventure.authService.repository.User;

/**
 * Couple 관련 Entity와 DTO 간의 변환을 담당하는 Mapper
 */
@Component
public class CoupleMapper {
    
    /**
     * CoupleRoom과 User를 CoupleMatchResponse로 변환
     */
    public CoupleMatchResponse toCoupleMatchResponse(CoupleRoom coupleRoom, User user) {
        return new CoupleMatchResponse(
            coupleRoom.getCoupleId(),
            coupleRoom.getCreatorUserId(),
            user.getProviderId(),
            null // JWT 토큰 없음
        );
    }
    
    /**
     * CoupleRoom과 User, JWT 토큰을 CoupleMatchResponse로 변환
     */
    public CoupleMatchResponse toCoupleMatchResponse(CoupleRoom coupleRoom, User user, String accessToken) {
        return new CoupleMatchResponse(
            coupleRoom.getCoupleId(),
            coupleRoom.getCreatorUserId(),
            user.getProviderId(),
            accessToken
        );
    }
    
    /**
     * CoupleRoom을 CoupleMatchResponse로 변환 (파트너 정보 없이)
     */
    public CoupleMatchResponse toCoupleMatchResponse(CoupleRoom coupleRoom) {
        return new CoupleMatchResponse(
            coupleRoom.getCoupleId(),
            coupleRoom.getCreatorUserId(),
            coupleRoom.getPartnerUserId(),
            null // JWT 토큰 없음
        );
    }
    
    /**
     * 초대 코드를 CreateCoupleRoomResponse로 변환
     */
    public CreateCoupleRoomResponse toCreateCoupleRoomResponse(String inviteCode) {
        return new CreateCoupleRoomResponse(inviteCode);
    }
}

