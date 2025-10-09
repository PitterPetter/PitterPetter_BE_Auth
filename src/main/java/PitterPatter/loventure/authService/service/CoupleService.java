package PitterPatter.loventure.authService.service;

import java.security.SecureRandom;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.f4b6a3.tsid.TsidCreator;

import PitterPatter.loventure.authService.dto.request.CreateCoupleRoomWithOnboardingRequest;
import PitterPatter.loventure.authService.dto.response.ApiResponse;
import PitterPatter.loventure.authService.dto.response.CoupleMatchResponse;
import PitterPatter.loventure.authService.dto.response.CreateCoupleRoomResponse;
import PitterPatter.loventure.authService.exception.BusinessException;
import PitterPatter.loventure.authService.exception.ErrorCode;
import PitterPatter.loventure.authService.mapper.CoupleMapper;
import PitterPatter.loventure.authService.repository.CoupleRoom;
import PitterPatter.loventure.authService.repository.CoupleRoomRepository;
import PitterPatter.loventure.authService.repository.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CoupleService {

    private final CoupleRoomRepository coupleRoomRepository;
    private final UserService userService;
    private final CoupleMapper coupleMapper;

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

            CoupleMatchResponse response = coupleMapper.toCoupleMatchResponse(coupleRoom, user);

            log.info("커플 매칭 완료 - coupleId: {}, partnerUserId: {}", coupleId, user.getProviderId());
            return ApiResponse.success(response, "커플 매칭 완료");
            
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
        return ApiResponse.success(null, "커플 매칭이 취소되었습니다. 다시 매칭할 수 있습니다.");
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
     * 데이트 시작일 문자열을 LocalDateTime으로 파싱
     * 지원 형식:
     * - yyyy.MM.dd (예: 2025.10.09)
     * - yyyy-MM-dd (예: 2025-10-09)
     * - yyyy-MM-ddTHH:mm:ss (예: 2025-10-09T00:00:00)
     */
    private java.time.LocalDateTime parseDatingStartDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            throw new IllegalArgumentException("데이트 시작일이 비어있습니다.");
        }
        
        String dateStr = dateString.trim();
        
        try {
            // 1. yyyy.MM.dd 형식 시도
            if (dateStr.matches("\\d{4}\\.\\d{2}\\.\\d{2}")) {
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd");
                return java.time.LocalDate.parse(dateStr, formatter).atStartOfDay();
            }
            // 2. yyyy-MM-dd 형식 시도
            else if (dateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return java.time.LocalDate.parse(dateStr).atStartOfDay();
            }
            // 3. ISO 형식 시도
            else {
                return java.time.LocalDateTime.parse(dateStr);
            }
        } catch (java.time.format.DateTimeParseException e) {
            throw new IllegalArgumentException(
                "날짜 형식이 올바르지 않습니다. 지원 형식: yyyy.MM.dd, yyyy-MM-dd, yyyy-MM-ddTHH:mm:ss", e);
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
    
}