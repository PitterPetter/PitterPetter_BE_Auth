package PitterPatter.loventure.authService.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import PitterPatter.loventure.authService.dto.response.RecommendationDataResponse;
import PitterPatter.loventure.authService.exception.BusinessException;
import PitterPatter.loventure.authService.exception.ErrorCode;
import PitterPatter.loventure.authService.repository.CoupleOnboarding;
import PitterPatter.loventure.authService.repository.CoupleOnboardingRepository;
import PitterPatter.loventure.authService.repository.CoupleRoom;
import PitterPatter.loventure.authService.repository.CoupleRoomRepository;
import PitterPatter.loventure.authService.repository.User;
import PitterPatter.loventure.authService.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * AI 서비스에서 사용할 사용자 및 커플 데이터를 제공하는 서비스
 * 비즈니스 로직과 예외 처리를 개선
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationDataService {
    
    private final UserRepository userRepository;
    private final CoupleRoomRepository coupleRoomRepository;
    private final CoupleOnboardingRepository coupleOnboardingRepository;
    
    /**
     * 사용자 ID로 사용자 및 커플 데이터 조회
     * 
     * @param userId 사용자 ID
     * @return RecommendationDataResponse
     * @throws BusinessException 사용자를 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public RecommendationDataResponse getRecommendationDataByUserId(String userId) {
        log.info("사용자 ID로 추천 데이터 조회 시작: {}", userId);
        
        try {
            // 사용자 조회
            User user = userRepository.findById(parseUserId(userId))
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
            
            // 사용자의 커플룸 조회
            CoupleRoom coupleRoom = findCoupleRoomByUserId(user.getProviderId());
            
            // 커플룸이 있는 경우 온보딩 정보 조회
            CoupleOnboarding coupleOnboarding = null;
            if (coupleRoom != null) {
                coupleOnboarding = findCoupleOnboardingByCoupleId(coupleRoom.getCoupleId());
            }
            
            RecommendationDataResponse response = RecommendationDataResponse.from(user, coupleRoom, coupleOnboarding);
            log.info("사용자 추천 데이터 조회 완료: userId={}, hasCouple={}", 
                    userId, coupleRoom != null);
            
            return response;
            
        } catch (NumberFormatException e) {
            log.error("잘못된 사용자 ID 형식: {}", userId, e);
            throw new BusinessException(ErrorCode.INVALID_USER_ID_FORMAT);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("추천 데이터 조회 중 오류 발생: userId={}", userId, e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Provider ID로 사용자 및 커플 데이터 조회
     * 
     * @param providerId Provider ID
     * @return RecommendationDataResponse
     * @throws BusinessException 사용자를 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public RecommendationDataResponse getRecommendationDataByProviderId(String providerId) {
        log.info("Provider ID로 추천 데이터 조회 시작: {}", providerId);
        
        try {
            // 사용자 조회
            User user = userRepository.findByProviderId(providerId);
            if (user == null) {
                throw new BusinessException(ErrorCode.USER_NOT_FOUND);
            }
            
            // 사용자의 커플룸 조회
            CoupleRoom coupleRoom = findCoupleRoomByUserId(providerId);
            
            // 커플룸이 있는 경우 온보딩 정보 조회
            CoupleOnboarding coupleOnboarding = null;
            if (coupleRoom != null) {
                coupleOnboarding = findCoupleOnboardingByCoupleId(coupleRoom.getCoupleId());
            }
            
            RecommendationDataResponse response = RecommendationDataResponse.from(user, coupleRoom, coupleOnboarding);
            log.info("Provider ID 추천 데이터 조회 완료: providerId={}, hasCouple={}", 
                    providerId, coupleRoom != null);
            
            return response;
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("추천 데이터 조회 중 오류 발생: providerId={}", providerId, e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * 사용자 ID로 커플룸 조회
     * 
     * @param userId 사용자 ID
     * @return CoupleRoom (없으면 null)
     */
    private CoupleRoom findCoupleRoomByUserId(String userId) {
        try {
            // 사용자가 생성자이거나 파트너인 활성 상태의 커플룸 조회
            Optional<CoupleRoom> coupleRoomOpt = coupleRoomRepository
                    .findByCreatorUserIdAndStatus(userId, CoupleRoom.CoupleStatus.ACTIVE)
                    .or(() -> coupleRoomRepository
                            .findByPartnerUserIdAndStatus(userId, CoupleRoom.CoupleStatus.ACTIVE));
            
            return coupleRoomOpt.orElse(null);
            
        } catch (Exception e) {
            log.warn("커플룸 조회 중 오류 발생: userId={}", userId, e);
            return null;
        }
    }
    
    /**
     * 커플 ID로 온보딩 정보 조회
     * 
     * @param coupleId 커플 ID
     * @return CoupleOnboarding (없으면 null)
     */
    private CoupleOnboarding findCoupleOnboardingByCoupleId(String coupleId) {
        try {
            return coupleOnboardingRepository.findByCoupleId(coupleId).orElse(null);
        } catch (Exception e) {
            log.warn("커플 온보딩 정보 조회 중 오류 발생: coupleId={}", coupleId, e);
            return null;
        }
    }
    
    /**
     * 문자열을 BigInteger로 변환
     * 
     * @param userIdStr 사용자 ID 문자열
     * @return BigInteger 사용자 ID
     * @throws NumberFormatException 잘못된 형식인 경우
     */
    private java.math.BigInteger parseUserId(String userIdStr) {
        try {
            return new java.math.BigInteger(userIdStr);
        } catch (NumberFormatException e) {
            log.error("잘못된 사용자 ID 형식: {}", userIdStr);
            throw e;
        }
    }
}