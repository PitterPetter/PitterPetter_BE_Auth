package PitterPatter.loventure.authService.service;

import java.math.BigInteger;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import PitterPatter.loventure.authService.dto.response.RecommendationDataResponse;
import PitterPatter.loventure.authService.repository.CoupleOnboarding;
import PitterPatter.loventure.authService.repository.CoupleOnboardingRepository;
import PitterPatter.loventure.authService.repository.CoupleRoom;
import PitterPatter.loventure.authService.repository.CoupleRoomRepository;
import PitterPatter.loventure.authService.repository.User;
import PitterPatter.loventure.authService.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationDataService {

    private final UserRepository userRepository;
    private final CoupleRoomRepository coupleRoomRepository;
    private final CoupleOnboardingRepository coupleOnboardingRepository;

    /**
     * 코스 추천을 위한 사용자 및 커플 온보딩 정보를 조회합니다.
     * @param userId 사용자 ID (BigInteger userId 값)
     * @return RecommendationDataResponse DTO
     * @throws RuntimeException 사용자(userId)를 찾을 수 없을 때 발생
     */
    public RecommendationDataResponse getRecommendationData(String userId) {
        // 1. 사용자 정보 조회 (userId 사용)
        Optional<User> userOpt = userRepository.findByUserId(new BigInteger(userId));
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found with userId: " + userId);
        }
        User user = userOpt.get();

        // 2. 사용자 정보 DTO 생성
        RecommendationDataResponse.UserData userData = RecommendationDataResponse.fromUserEntity(user);

        // 3. 커플 정보 조회 (CoupleRoom 및 CoupleOnboarding)
        RecommendationDataResponse.CoupleData coupleData = null;

        // 사용자의 providerId를 사용하여 ACTIVE 상태의 CoupleRoom 조회 (creator이거나 partner일 수 있음)
        Optional<CoupleRoom> coupleRoomOpt = coupleRoomRepository.findByCreatorUserIdAndStatus(user.getProviderId(), CoupleRoom.CoupleStatus.ACTIVE)
                .or(() -> coupleRoomRepository.findByPartnerUserIdAndStatus(user.getProviderId(), CoupleRoom.CoupleStatus.ACTIVE));

        if (coupleRoomOpt.isPresent()) {
            String coupleId = coupleRoomOpt.get().getCoupleId();

            // 4. 커플 ID로 CoupleOnboarding 정보 조회
            Optional<CoupleOnboarding> coupleOnboardingOpt = coupleOnboardingRepository.findByCoupleId(coupleId);

            if (coupleOnboardingOpt.isPresent()) {
                CoupleOnboarding coupleOnboarding = coupleOnboardingOpt.get();
                // 5. 커플 정보 DTO 생성
                coupleData = RecommendationDataResponse.fromCoupleOnboardingAndId(coupleOnboarding, coupleId);
            }
            // 참고: 커플방은 있지만, 커플 온보딩 정보가 아직 없는 경우 (coupleData는 null)
        }

        // 6. 최종 응답 DTO 반환
        return RecommendationDataResponse.builder()
                .userData(userData)
                .coupleData(coupleData)
                .build();
    }
}