package PitterPatter.loventure.authService.mapper;

import java.util.Optional;

import org.springframework.stereotype.Component;

import PitterPatter.loventure.authService.dto.response.MyPageResponse;
import PitterPatter.loventure.authService.repository.CoupleRoom;
import PitterPatter.loventure.authService.repository.User;
import lombok.RequiredArgsConstructor;

/**
 * MyPage 관련 Entity와 DTO 간의 변환을 담당하는 Mapper
 */
@Component
@RequiredArgsConstructor
public class MyPageMapper {

    /**
     * User와 CoupleRoom을 MyPageResponse로 변환
     */
    public MyPageResponse toMyPageResponse(User user, Optional<CoupleRoom> coupleRoomOpt, User partner) {
        MyPageResponse.CoupleInfo coupleInfo = null;
        
        if (coupleRoomOpt.isPresent()) {
            CoupleRoom coupleRoom = coupleRoomOpt.get();
            coupleInfo = new MyPageResponse.CoupleInfo(
                coupleRoom.getCoupleId(),
                coupleRoom.getCoupleHomeName(),
                coupleRoom.getDatingStartDate(),
                partner != null ? partner.getName() : null,
                partner != null ? partner.getEmail() : null,
                coupleRoom.getCreatorUserId().equals(user.getProviderId()) 
                    ? coupleRoom.getPartnerUserId() 
                    : coupleRoom.getCreatorUserId(),
                coupleRoom.getStatus().name(),
                coupleRoom.getCreatedAt()
            );
        }

        return new MyPageResponse(
            user.getUserId(),
            user.getEmail(),
            user.getName(),
            user.getProviderType(),
            user.getProviderId(),
            user.getStatus(),
            user.getBirthDate(),
            user.getGender(),
            user.getAlcoholPreference(),
            user.getActiveBound(),
            user.getFavoriteFoodCategories(),
            user.getDateCostPreference(),
            user.getPreferredAtmosphere(),
            user.getCreatedAt(),
            user.getUpdatedAt(),
            user.getRerollCount(),
            coupleInfo
        );
    }
}
