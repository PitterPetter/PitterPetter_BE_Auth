package PitterPatter.loventure.authService.service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.f4b6a3.tsid.TsidCreator;

import PitterPatter.loventure.authService.dto.request.CoupleUpdateRequest;
import PitterPatter.loventure.authService.dto.request.CreateCoupleRoomWithOnboardingRequest;
import PitterPatter.loventure.authService.dto.response.ApiResponse;
import PitterPatter.loventure.authService.dto.response.CoupleMatchResponse;
import PitterPatter.loventure.authService.dto.response.CreateCoupleRoomResponse;
import PitterPatter.loventure.authService.dto.response.RecommendationCoupleResponse;
import PitterPatter.loventure.authService.dto.response.RecommendationDataResponse;
import PitterPatter.loventure.authService.dto.response.RecommendationUserResponse;
import PitterPatter.loventure.authService.exception.BusinessException;
import PitterPatter.loventure.authService.exception.ErrorCode;
import PitterPatter.loventure.authService.mapper.CoupleMapper;
import PitterPatter.loventure.authService.repository.CoupleRoom;
import PitterPatter.loventure.authService.repository.CoupleRoomRepository;
import PitterPatter.loventure.authService.repository.DateCostPreference;
import PitterPatter.loventure.authService.repository.User;
import PitterPatter.loventure.authService.repository.UserRepository;
import PitterPatter.loventure.authService.security.JWTUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CoupleService {

    private final CoupleRoomRepository coupleRoomRepository;
    private final UserService userService;
    private final CoupleMapper coupleMapper;
    private final JWTUtil jwtUtil;
    private final UserRepository userRepository;
    private final TicketService ticketService;

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 6;

    // 에러 코드 상수는 ErrorConstants에서 관리

    /**
     * 커플룸 생성 (초대코드만 생성)
     */
    @Transactional
    public ApiResponse<CreateCoupleRoomResponse> createCoupleRoom(String providerId) {

        try {
            User user = userService.validateUserByProviderId(providerId);
            // 사용자가 이미 커플 상태인지 확인
            if (isUserAlreadyCoupled(user.getProviderId())) {
                throw new BusinessException(ErrorCode.ALREADY_COUPLED, "이미 커플 상태입니다.");
            }

            String inviteCode = generateInviteCode();

            // 임시 커플룸 생성 (coupleId는 매칭 시 생성)
            CoupleRoom coupleRoom = CoupleRoom.builder()
                    .coupleId(null) // 매칭 시 생성
                    .inviteCode(inviteCode)
                    .creatorUserId(providerId)
                    // status는 기본값 PENDING 사용
                    .build();
            coupleRoomRepository.save(coupleRoom);

            CreateCoupleRoomResponse response = coupleMapper.toCreateCoupleRoomResponse(inviteCode);

            log.info("커플룸 생성 완료 - inviteCode: {}, creatorUserId: {}", inviteCode, providerId);
            return ApiResponse.success(response);
        
        } catch (BusinessException e) {
            return ApiResponse.error(e.getErrorCode().getCode(), e.getMessage());
        }
    }

    /**
     * 커플 매칭
     */
    @Transactional
    public ApiResponse<CoupleMatchResponse> matchCouple(String providerId, String inviteCode) {
        try {
            User user = userService.validateUserByProviderId(providerId);

            CoupleRoom coupleRoom = validateAndGetCoupleRoom(inviteCode, user.getProviderId());
            if (coupleRoom == null) {
                return ApiResponse.error(ErrorCode.INVITE_CODE_NOT_FOUND.getCode(), "초대 코드가 존재하지 않습니다.");
            }

            // 매칭 시 coupleId 생성
            String coupleId = generateTSID();
            coupleRoom.setCoupleId(coupleId);
            coupleRoom.setPartnerUserId(user.getProviderId());
            coupleRoom.setStatus(CoupleRoom.CoupleStatus.ACTIVE); // 매칭 완료 시 ACTIVE로 변경
            coupleRoomRepository.save(coupleRoom);

            // 커플 매칭 완료 후 새 JWT 생성 (coupleId 포함)
            String newJwt = jwtUtil.createJwtWithUserIdAndCoupleId(
                providerId, 
                user.getUserId(), 
                coupleId, 
                600000L // 10분 (600000ms)
            );

            CoupleMatchResponse response = coupleMapper.toCoupleMatchResponse(coupleRoom, user, newJwt);

            log.info("커플 매칭 완료 - coupleId: {}, partnerUserId: {}, 새 JWT 발급 완료", coupleId, user.getProviderId());
            return ApiResponse.success("커플 매칭 완료", response);
            
        } catch (BusinessException e) {
            return ApiResponse.error(e.getErrorCode().getCode(), e.getMessage());
        }
    }

    /**
     * 커플룸 검증 및 조회
     */
    private CoupleRoom validateAndGetCoupleRoom(String inviteCode, String providerId) {
        log.debug("초대코드 검색 시작: inviteCode={}, providerId={}", inviteCode, providerId);
        
        Optional<CoupleRoom> coupleRoomOpt = coupleRoomRepository.findByInviteCode(inviteCode);
        
        if (coupleRoomOpt.isEmpty()) {
            log.warn("초대코드를 찾을 수 없음: {}", inviteCode);
            return null;
        }

        CoupleRoom coupleRoom = coupleRoomOpt.get();

        // PENDING 상태가 아니면 매칭할 수 없음
        if (coupleRoom.getStatus() != CoupleRoom.CoupleStatus.PENDING) {
            throw new BusinessException(ErrorCode.ALREADY_MATCHED_CODE, "매칭 대기 중이지 않은 초대 코드입니다.");
        }

        if (coupleRoom.getCreatorUserId().equals(providerId)) {
            throw new BusinessException(ErrorCode.ALREADY_MATCHED_CODE, "자기 자신과는 매칭할 수 없습니다.");
        }

        if (coupleRoom.getPartnerUserId() != null) {
            throw new BusinessException(ErrorCode.ALREADY_MATCHED_CODE, "이미 다른 사용자와 매칭된 초대 코드입니다.");
        }

        return coupleRoom;
    }

    /**
     * 커플 매칭 취소 (PENDING 상태로 되돌림)
     */
    @Transactional
    public ApiResponse<Void> cancelCouple(String coupleId) {
        Optional<CoupleRoom> coupleRoomOpt = coupleRoomRepository.findByCoupleId(coupleId);
        if (coupleRoomOpt.isEmpty()) {
            return ApiResponse.error(ErrorCode.COUPLE_NOT_FOUND.getCode(), "존재하지 않는 커플입니다.");
        }

        CoupleRoom coupleRoom = coupleRoomOpt.get();
        if (coupleRoom.getStatus() == CoupleRoom.CoupleStatus.PENDING) {
            return ApiResponse.error(ErrorCode.ALREADY_CANCELLED.getCode(), "이미 매칭 대기 상태입니다.");
        }

        // 매칭 취소 시 PENDING 상태로 되돌리고 파트너 정보 제거
        coupleRoom.setStatus(CoupleRoom.CoupleStatus.PENDING);
        coupleRoom.setPartnerUserId(null);
        coupleRoomRepository.save(coupleRoom);
        log.info("커플 매칭 취소 완료 - coupleId: {}, 상태: PENDING으로 변경", coupleId);
        return ApiResponse.success("커플 매칭이 취소되었습니다. 다시 매칭할 수 있습니다.", null);
    }


    // 매칭을 위한 inviteCode 생성
    private String generateInviteCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        do {
            code.setLength(0);
            for (int i = 0; i < CODE_LENGTH; i++) {
                code.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
            }
        } while (coupleRoomRepository.existsByInviteCode(code.toString()));
        return code.toString();
    }

    /**
     * 데이트 시작일 문자열을 LocalDate로 파싱
     * 지원 형식:
     * - yyyy.MM.dd (예: 2025.10.09)
     * - yyyy-MM-dd (예: 2025-10-09)
     */
    private LocalDate parseDatingStartDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }
        
        String dateStr = dateString.trim();
        
        try {
            // 1. yyyy.MM.dd 형식 시도
            if (dateStr.matches("\\d{4}\\.\\d{2}\\.\\d{2}")) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
                return LocalDate.parse(dateStr, formatter);
            }
            // 2. yyyy-MM-dd 형식 시도
            else if (dateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return LocalDate.parse(dateStr);
            }
            else {
                throw new IllegalArgumentException("지원하지 않는 날짜 형식입니다: " + dateStr);
            }
        } catch (DateTimeParseException e) {
            log.error("데이트 시작일 파싱 오류: {}", dateString, e);
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "잘못된 데이트 시작일 형식입니다. (yyyy.MM.dd 또는 yyyy-MM-dd)");
        }
    }

    private String generateTSID() {
        return TsidCreator.getTsid().toString();
    }

    /**
     * 사용자가 이미 커플 상태인지 확인
     */
    private boolean isUserAlreadyCoupled(String providerId) {
        // 사용자가 생성자이거나 파트너인 활성 상태의 커플룸이 있는지 확인
        return coupleRoomRepository.existsByCreatorUserIdAndStatus(providerId, CoupleRoom.CoupleStatus.ACTIVE) ||
               coupleRoomRepository.existsByPartnerUserIdAndStatus(providerId, CoupleRoom.CoupleStatus.ACTIVE) ||
               coupleRoomRepository.existsByCreatorUserIdAndStatus(providerId, CoupleRoom.CoupleStatus.PENDING) ||
               coupleRoomRepository.existsByPartnerUserIdAndStatus(providerId, CoupleRoom.CoupleStatus.PENDING);
    }

    /**
     * 사용자의 커플 매칭 상태 확인 (public 메서드)
     */
    public boolean isUserCoupled(String providerId) {
        return isUserAlreadyCoupled(providerId);
    }
    
    /**
     * 사용자의 커플 정보 조회
     */
    public Optional<CoupleRoom> getCoupleInfo(String providerId) {
        return coupleRoomRepository.findByCreatorUserIdAndStatus(providerId, CoupleRoom.CoupleStatus.ACTIVE)
                .or(() -> coupleRoomRepository.findByPartnerUserIdAndStatus(providerId, CoupleRoom.CoupleStatus.ACTIVE));
    }
    
    /**
     * 사용자의 커플 ID 조회 (JWT에 coupleId가 없는 경우 사용)
     */
    public String getCoupleIdByProviderId(String providerId) {
        Optional<CoupleRoom> coupleRoomOpt = getCoupleInfo(providerId);
        return coupleRoomOpt.map(CoupleRoom::getCoupleId).orElse(null);
    }
    
    /**
     * 커플룸 생성과 온보딩을 함께 처리하는 통합 메서드
     */
    @Transactional
    public ApiResponse<CreateCoupleRoomResponse> createCoupleRoomWithOnboarding(String providerId, CreateCoupleRoomWithOnboardingRequest request) {
        try {
            User user = userService.validateUserByProviderId(providerId);
            
            // 사용자가 이미 커플 상태인지 확인
            if (isUserAlreadyCoupled(user.getProviderId())) {
                throw new BusinessException(ErrorCode.ALREADY_COUPLED, "이미 커플 상태입니다.");
            }

            String inviteCode = generateInviteCode();
            String coupleId = TsidCreator.getTsid().toString();

            // 커플룸 생성 (PENDING 상태로 생성 - 매칭 대기)
            CoupleRoom coupleRoom = CoupleRoom.builder()
                    .coupleId(coupleId)
                    .inviteCode(inviteCode)
                    .creatorUserId(providerId)
                    .coupleHomeName(request.coupleHomeName())
                    .datingStartDate(parseDatingStartDate(request.datingStartDate()))
                    .status(CoupleRoom.CoupleStatus.PENDING)
                    .build();
            coupleRoomRepository.save(coupleRoom);

            CreateCoupleRoomResponse response = coupleMapper.toCreateCoupleRoomResponse(inviteCode);

            log.info("커플룸 생성 완료 - coupleId: {}, inviteCode: {}, creatorUserId: {}, coupleHomeName: {}, datingStartDate: {}", 
                    coupleId, inviteCode, providerId, request.coupleHomeName(), request.datingStartDate());
            return ApiResponse.success(response);
        
        } catch (BusinessException e) {
            return ApiResponse.error(e.getErrorCode().getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("커플룸 생성 및 온보딩 중 오류 발생: {}", e.getMessage(), e);
            return ApiResponse.error("50001", "커플룸 생성 중 오류가 발생했습니다.");
        }
    }
    
    /**
     * 커플 정보 변경
     */
    @Transactional
    public ApiResponse<Void> updateCoupleInfo(String providerId, CoupleUpdateRequest request) {
        try {
            // 사용자 검증
            userService.validateUserByProviderId(providerId);
            
            // 사용자의 커플 정보 조회
            Optional<CoupleRoom> coupleRoomOpt = getCoupleInfo(providerId);
            if (coupleRoomOpt.isEmpty()) {
                return ApiResponse.error(ErrorCode.COUPLE_NOT_FOUND.getCode(), "커플 정보를 찾을 수 없습니다.");
            }
            
            CoupleRoom coupleRoom = coupleRoomOpt.get();
            
            // 커플홈 이름 변경
            if (request.coupleHomeName() != null && !request.coupleHomeName().trim().isEmpty()) {
                coupleRoom.setCoupleHomeName(request.coupleHomeName().trim());
            }
            
            // 데이트 시작일 변경
            if (request.datingStartDate() != null && !request.datingStartDate().trim().isEmpty()) {
                coupleRoom.setDatingStartDate(parseDatingStartDate(request.datingStartDate()));
            }
            
            coupleRoomRepository.save(coupleRoom);
            
            log.info("커플 정보 변경 완료 - coupleId: {}, coupleHomeName: {}, datingStartDate: {}", 
                    coupleRoom.getCoupleId(), coupleRoom.getCoupleHomeName(), coupleRoom.getDatingStartDate());
            
            return ApiResponse.success("커플 정보가 성공적으로 변경되었습니다.", null);
            
        } catch (BusinessException e) {
            return ApiResponse.error(e.getErrorCode().getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("커플 정보 변경 중 오류 발생: {}", e.getMessage(), e);
            return ApiResponse.error("50001", "커플 정보 변경 중 오류가 발생했습니다.");
        }
    }
    
    /**
     * AI 서버용 커플 추천 데이터 조회
     */
    public ApiResponse<RecommendationDataResponse> getRecommendationData(String coupleId) {
        try {
            // 커플룸 조회
            Optional<CoupleRoom> coupleRoomOpt = coupleRoomRepository.findByCoupleId(coupleId);
            if (coupleRoomOpt.isEmpty()) {
                return ApiResponse.error(ErrorCode.COUPLE_NOT_FOUND.getCode(), "존재하지 않는 커플입니다.");
            }
            
            CoupleRoom coupleRoom = coupleRoomOpt.get();
            
            // 커플 상태 확인
            if (coupleRoom.getStatus() != CoupleRoom.CoupleStatus.ACTIVE) {
                return ApiResponse.error(ErrorCode.COUPLE_NOT_FOUND.getCode(), "활성화되지 않은 커플입니다.");
            }
            
            // reroll 관리 로직
            manageRerollCount(coupleRoom);
            
            // 사용자 정보 조회 (생성자)
            User creatorUser = userService.getUserByProviderId(coupleRoom.getCreatorUserId());
            if (creatorUser == null) {
                return ApiResponse.error(ErrorCode.USER_NOT_FOUND.getCode(), "생성자 사용자 정보를 찾을 수 없습니다.");
            }
            
            // 파트너 사용자 정보 조회
            User partnerUser = null;
            if (coupleRoom.getPartnerUserId() != null) {
                partnerUser = userService.getUserByProviderId(coupleRoom.getPartnerUserId());
                if (partnerUser == null) {
                    return ApiResponse.error(ErrorCode.USER_NOT_FOUND.getCode(), "파트너 사용자 정보를 찾을 수 없습니다.");
                }
            }
            
            // 사용자 응답 데이터 생성
            RecommendationUserResponse userResponse = createRecommendationUserResponse(creatorUser);
            RecommendationUserResponse partnerResponse = partnerUser != null ? 
                createRecommendationUserResponse(partnerUser) : null;
            
            // 커플 응답 데이터 생성
            RecommendationCoupleResponse coupleResponse = createRecommendationCoupleResponse(coupleRoom);
            
            // 최종 응답 생성
            RecommendationDataResponse response = new RecommendationDataResponse(
                userResponse,
                partnerResponse,
                coupleResponse
            );
            
            log.info("커플 추천 데이터 조회 성공 - coupleId: {}", coupleId);
            return ApiResponse.success(response);
            
        } catch (Exception e) {
            log.error("커플 추천 데이터 조회 중 오류 발생 - coupleId: {}, error: {}", coupleId, e.getMessage(), e);
            return ApiResponse.error("50001", "커플 추천 데이터 조회 중 오류가 발생했습니다.");
        }
    }
    
    /**
     * 사용자 응답 데이터 생성
     */
    private RecommendationUserResponse createRecommendationUserResponse(User user) {
        return new RecommendationUserResponse(
            user.getUserId(),
            user.getName(),
            user.getBirthDate() != null ? user.getBirthDate().toString() : null,
            user.getGender() != null ? user.getGender().toString() : null,
            user.getAlcoholPreference() != null && user.getAlcoholPreference() > 0,
            user.getActiveBound() != null && user.getActiveBound() > 0,
            user.getFavoriteFoodCategories() != null && !user.getFavoriteFoodCategories().isEmpty() ? 
                user.getFavoriteFoodCategories().get(0).toString() : null,
            getDateCostValue(user.getDateCostPreference()),
            user.getPreferredAtmosphere(),
            user.getUserId(), // uuid 대신 userId 사용
            user.getStatus() != null ? user.getStatus().toString() : null,
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }
    
    /**
     * DateCostPreference를 Integer 값으로 변환
     */
    private Integer getDateCostValue(DateCostPreference dateCostPreference) {
        if (dateCostPreference == null) {
            return null;
        }
        
        return switch (dateCostPreference) {
            case 만원_미만 -> 10000;
            case 만원_삼만원 -> 20000;
            case 삼만원_오만원 -> 40000;
            case 오만원_팔만원 -> 65000;
            case 팔만원_이상 -> 100000;
        };
    }
    
    /**
     * reroll 카운트 관리
     * - 매일 자정에 3으로 초기화
     * - API 요청 시마다 1씩 감소
     */
    @Transactional
    protected void manageRerollCount(CoupleRoom coupleRoom) {
        LocalDate today = LocalDate.now();
        LocalDate lastResetDate = coupleRoom.getLastRerollResetDate();
        
        // 마지막 리셋 날짜가 없거나 오늘과 다르면 리셋
        if (lastResetDate == null || !lastResetDate.equals(today)) {
            coupleRoom.setRerollCount(3);
            coupleRoom.setLastRerollResetDate(today);
            coupleRoomRepository.save(coupleRoom);
            log.info("reroll 카운트 리셋 - coupleId: {}, rerollCount: 3", coupleRoom.getCoupleId());
        }
        
        // 현재 reroll 카운트가 0보다 크면 1 감소
        if (coupleRoom.getRerollCount() > 0) {
            coupleRoom.setRerollCount(coupleRoom.getRerollCount() - 1);
            coupleRoomRepository.save(coupleRoom);
            log.info("reroll 카운트 감소 - coupleId: {}, 남은 reroll: {}", 
                    coupleRoom.getCoupleId(), coupleRoom.getRerollCount());
        } else {
            log.warn("reroll 카운트 부족 - coupleId: {}, rerollCount: {}", 
                    coupleRoom.getCoupleId(), coupleRoom.getRerollCount());
        }
    }
    
    /**
     * 커플 응답 데이터 생성
     */
    private RecommendationCoupleResponse createRecommendationCoupleResponse(CoupleRoom coupleRoom) {
        return new RecommendationCoupleResponse(
            coupleRoom.getCoupleId(),
            coupleRoom.getCreatorUserId(),
            coupleRoom.getPartnerUserId(),
            coupleRoom.getCoupleHomeName(),
            coupleRoom.getRerollCount(), // reroll - CoupleRoom의 rerollCount 사용
            0, // ticket - CoupleRoom에 없으므로 기본값
            0, // loveDay - CoupleRoom에 없으므로 기본값
            0  // diaryCount - CoupleRoom에 없으므로 기본값
        );
    }

    /**
     * HttpServletRequest에서 coupleId 추출 (JWT에서)
     */
    public String getCoupleIdFromRequest(jakarta.servlet.http.HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        
        // JWT에서 coupleId 추출
        String coupleId = jwtUtil.getCoupleIdFromToken(token);
        if (coupleId == null) {
            throw new IllegalArgumentException("JWT에서 coupleId를 찾을 수 없습니다");
        }
        
        return coupleId;
    }
    
    /**
     * HttpServletRequest에서 Authorization 헤더의 Bearer 토큰 추출
     */
    private String extractTokenFromRequest(jakarta.servlet.http.HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        throw new IllegalArgumentException("Authorization 헤더에서 Bearer 토큰을 찾을 수 없습니다");
    }

    /**
     * 커플룸의 두 사용자 모두 rock 상태를 완료로 변경하고 티켓 차감
     */
    @Transactional
    public void completeRockStatusForCouple(String coupleId) {
        CoupleRoom coupleRoom = coupleRoomRepository.findByCoupleId(coupleId)
                .orElseThrow(() -> new IllegalArgumentException("커플룸을 찾을 수 없습니다: " + coupleId));
        
        // 1. 두 사용자 모두 상태 변경
        completeRockStatus(coupleRoom.getCreatorUserId());
        if (coupleRoom.getPartnerUserId() != null) {
            completeRockStatus(coupleRoom.getPartnerUserId());
        }
        
        // 2. 커플룸 상태 업데이트
        coupleRoom.setIsRockCompleted(true);
        coupleRoom.setRockCompletedAt(LocalDateTime.now());
        coupleRoomRepository.save(coupleRoom);
        
        // 3. 지역락 해제를 위한 티켓 차감 (2개 → 0개)
        ticketService.deductTicketsForRockRelease(coupleId);
        
        log.info("🎫 지역락 해제 완료 - coupleId: {}, 사용자 상태 변경 및 티켓 차감 완료", coupleId);
    }

    /**
     * 사용자의 rock 상태를 완료로 변경
     */
    @Transactional
    public void completeRockStatus(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
        user.setIsRockCompleted(true);
        user.setRockCompletedAt(LocalDateTime.now());
        userRepository.save(user);
    }

}