package PitterPatter.loventure.authService.service;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.f4b6a3.tsid.TsidCreator;

import PitterPatter.loventure.authService.dto.request.CoupleMatchRequest;
import PitterPatter.loventure.authService.dto.request.CoupleOnboardingRequest;
import PitterPatter.loventure.authService.dto.response.ApiResponse;
import PitterPatter.loventure.authService.dto.response.CoupleMatchResponse;
import PitterPatter.loventure.authService.dto.response.CreateCoupleRoomResponse;
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

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 6;

    // 에러 코드 상수
    private static final String ERROR_INVITE_CODE_NOT_FOUND = "40402";
    private static final String ERROR_COUPLE_NOT_FOUND = "40403";
    private static final String ERROR_ALREADY_COUPLED = "40005";
    private static final String ERROR_ALREADY_MATCHED_CODE = "40902";
    private static final String ERROR_ALREADY_CANCELLED = "40903";

    /**
     * 커플룸 생성 (초대코드만 생성)
     */
    @Transactional
    public ApiResponse<CreateCoupleRoomResponse> createCoupleRoom(String providerId) {

        try {
            User user = userService.validateUserByProviderId(providerId);
            // 사용자가 이미 커플 상태인지 확인
            if (isUserAlreadyCoupled(user.getProviderId())) {
                throw new IllegalArgumentException("이미 커플 상태입니다.");
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

            CreateCoupleRoomResponse response = CreateCoupleRoomResponse.builder()
                    .inviteCode(inviteCode)
                    .build();

            log.info("커플룸 생성 완료 - inviteCode: {}, creatorUserId: {}", inviteCode, providerId);
            return ApiResponse.success(response);
        
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(ERROR_ALREADY_COUPLED, e.getMessage());
        }
    }

    /**
     * 커플 매칭
     */
    @Transactional
    public ApiResponse<CoupleMatchResponse> matchCouple(CoupleMatchRequest request) {
        String userId = request.getUserId();
        String inviteCode = request.getInviteCode();

        try {
            User user = userService.validateUserByTsid(userId);

            CoupleRoom coupleRoom = validateAndGetCoupleRoom(inviteCode, user.getProviderId());
            if (coupleRoom == null) {
                return ApiResponse.error(ERROR_INVITE_CODE_NOT_FOUND, "초대 코드가 존재하지 않습니다.");
            }

            // 매칭 시 coupleId 생성
            String coupleId = generateTSID();
            coupleRoom.setCoupleId(coupleId);
            coupleRoom.setPartnerUserId(user.getProviderId());
            coupleRoom.setStatus(CoupleRoom.CoupleStatus.ACTIVE); // 매칭 완료 시 ACTIVE로 변경
            coupleRoomRepository.save(coupleRoom);

            CoupleMatchResponse response = CoupleMatchResponse.builder()
                    .coupleId(coupleId)
                    .creatorUserId(coupleRoom.getCreatorUserId())
                    .partnerUserId(user.getProviderId())
                    .build();

            log.info("커플 매칭 완료 - coupleId: {}, partnerUserId: {}", coupleId, user.getProviderId());
            return ApiResponse.success(response, "커플 매칭 완료");
            
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(ERROR_ALREADY_MATCHED_CODE, e.getMessage());
        }
    }

    /**
     * 커플룸 검증 및 조회
     */
    private CoupleRoom validateAndGetCoupleRoom(String inviteCode, String providerId) {
        log.info("초대코드 검색 시작: inviteCode={}, providerId={}", inviteCode, providerId);
        
        // 모든 커플룸 조회해서 디버깅
        List<CoupleRoom> allRooms = coupleRoomRepository.findAll();
        log.info("전체 커플룸 수: {}", allRooms.size());
        for (CoupleRoom room : allRooms) {
            log.info("커플룸: inviteCode={}, status={}", room.getInviteCode(), room.getStatus());
        }
        
        Optional<CoupleRoom> coupleRoomOpt = coupleRoomRepository.findByInviteCode(inviteCode);
        log.info("초대코드 검색 결과: found={}", coupleRoomOpt.isPresent());
        
        if (coupleRoomOpt.isEmpty()) {
            log.warn("초대코드를 찾을 수 없음: {}", inviteCode);
            return null;
        }

        CoupleRoom coupleRoom = coupleRoomOpt.get();

        // PENDING 상태가 아니면 매칭할 수 없음
        if (coupleRoom.getStatus() != CoupleRoom.CoupleStatus.PENDING) {
            throw new IllegalArgumentException("매칭 대기 중이지 않은 초대 코드입니다.");
        }

        if (coupleRoom.getCreatorUserId().equals(providerId)) {
            throw new IllegalArgumentException("자기 자신과는 매칭할 수 없습니다.");
        }

        if (coupleRoom.getPartnerUserId() != null) {
            throw new IllegalArgumentException("이미 다른 사용자와 매칭된 초대 코드입니다.");
        }

        return coupleRoom;
    }

    /**
     * 커플 매칭 취소
     */
    @Transactional
    public ApiResponse<Void> cancelCouple(String coupleId) {
        Optional<CoupleRoom> coupleRoomOpt = coupleRoomRepository.findByCoupleId(coupleId);
        if (coupleRoomOpt.isEmpty()) {
            return ApiResponse.error(ERROR_COUPLE_NOT_FOUND, "존재하지 않는 커플입니다.");
        }

        CoupleRoom coupleRoom = coupleRoomOpt.get();
        if (coupleRoom.getStatus() == CoupleRoom.CoupleStatus.DEACTIVED) {
            return ApiResponse.error(ERROR_ALREADY_CANCELLED, "커플 상태가 이미 해제되었습니다.");
        }

        coupleRoom.setStatus(CoupleRoom.CoupleStatus.DEACTIVED);
        coupleRoomRepository.save(coupleRoom);
        log.info("커플 매칭 취소 완료 - coupleId: {}", coupleId);
        return ApiResponse.success(null, "커플 매칭이 취소되었습니다.");
    }

    /**
     * 커플 온보딩 생성/수정
     */
    @Transactional
    public ApiResponse<Void> createOrUpdateOnboarding(String coupleId, CoupleOnboardingRequest request) {
        Optional<CoupleRoom> coupleRoomOpt = coupleRoomRepository.findByCoupleId(coupleId);
        if (coupleRoomOpt.isEmpty()) {
            return ApiResponse.error(ERROR_COUPLE_NOT_FOUND, "존재하지 않는 커플입니다.");
        }

        // CoupleRoom에 coupleHomeName과 datingStartDate 저장
        CoupleRoom coupleRoom = coupleRoomOpt.get();
        coupleRoom.setCoupleHomeName(request.getCoupleHomeName());
        coupleRoom.setDatingStartDate(request.getDatingStartDate());
        coupleRoomRepository.save(coupleRoom);

        log.info("커플 온보딩 저장 완료 - coupleId: {}, coupleHomeName: {}, datingStartDate: {}", 
                coupleId, request.getCoupleHomeName(), request.getDatingStartDate());
        return ApiResponse.success(null, "커플 온보딩 데이터 저장 완료");
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
}