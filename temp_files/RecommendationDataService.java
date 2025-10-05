package PitterPatter.loventure.authService.service;

import org.springframework.stereotype.Service;

import PitterPatter.loventure.authService.dto.response.RecommendationDataResponse;
import PitterPatter.loventure.authService.exception.BusinessException;
import PitterPatter.loventure.authService.exception.ErrorCode;
import PitterPatter.loventure.authService.repository.CoupleRoom;
import PitterPatter.loventure.authService.repository.CoupleRoomRepository;
import PitterPatter.loventure.authService.repository.User;
import PitterPatter.loventure.authService.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * AI 서비스를 위한 추천 데이터 제공 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationDataService {

    private final UserRepository userRepository;
    private final CoupleRoomRepository coupleRoomRepository;

    /**
     * 사용자 ID로 추천 데이터 조회
     */
    public RecommendationDataResponse getRecommendationData(String userId) {
        try {
            // 사용자 조회
            User user = userRepository.findByUserId(new java.math.BigInteger(userId))
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND_BY_ID));

            // 사용자 데이터 구성
            RecommendationDataResponse.UserData userData = new RecommendationDataResponse.UserData(
                    user.getUserId().toString(),
                    user.getName(),
                    user.getAlcoholPreference() != null ? user.getAlcoholPreference().toString() : null,
                    user.getActiveBound() != null ? user.getActiveBound().toString() : null,
                    user.getFavoriteFoodCategories().stream()
                            .map(Enum::name)
                            .toList(),
                    user.getDateCostPreference() != null ? user.getDateCostPreference().name() : null,
                    user.getPreferredAtmosphere()
            );

            // 커플 데이터 조회
            RecommendationDataResponse.CoupleData coupleData = null;
            CoupleRoom coupleRoom = coupleRoomRepository.findByCreatorUserIdOrPartnerUserIdAndStatus(
                    user.getProviderId(), CoupleRoom.CoupleStatus.ACTIVE)
                    .orElse(null);

            if (coupleRoom != null) {
                coupleData = new RecommendationDataResponse.CoupleData(
                        coupleRoom.getCoupleId(),
                        coupleRoom.getCoupleHomeName(),
                        coupleRoom.getDatingStartDate() != null ? coupleRoom.getDatingStartDate().toString() : null
                );
            }

            return new RecommendationDataResponse(userData, coupleData);

        } catch (NumberFormatException e) {
            log.warn("잘못된 사용자 ID 형식: {}", userId);
            throw new BusinessException(ErrorCode.INVALID_USER_ID_FORMAT);
        } catch (Exception e) {
            log.error("추천 데이터 조회 중 오류 발생: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.RECOMMENDATION_DATA_ERROR);
        }
    }
}
