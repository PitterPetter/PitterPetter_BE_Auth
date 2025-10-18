package PitterPatter.loventure.authService.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import PitterPatter.loventure.authService.dto.TicketInfo;
import PitterPatter.loventure.authService.dto.request.CoupleMatchRequest;
import PitterPatter.loventure.authService.dto.request.CoupleUpdateRequest;
import PitterPatter.loventure.authService.dto.request.CreateCoupleRoomWithOnboardingRequest;
import PitterPatter.loventure.authService.dto.response.ApiResponse;
import PitterPatter.loventure.authService.dto.response.CoupleMatchResponse;
import PitterPatter.loventure.authService.dto.response.CreateCoupleRoomResponse;
import PitterPatter.loventure.authService.dto.response.RecommendationDataResponse;
import PitterPatter.loventure.authService.service.CoupleService;
import PitterPatter.loventure.authService.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/couples")
@RequiredArgsConstructor
@Slf4j
public class CouplesController {
    
    private final CoupleService coupleService;
    private final UserService userService;

    // 커플룸 생성과 온보딩을 함께 처리하는 통합 API
    @PostMapping("/room")
    public ResponseEntity<ApiResponse<CreateCoupleRoomResponse>> createCoupleRoomWithOnboarding(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid CreateCoupleRoomWithOnboardingRequest request) {
        
        try {
            // JWT에서 providerId 추출
            String providerId = userService.extractProviderId(userDetails);
            // 커플룸 생성과 온보딩을 함께 처리
            ApiResponse<CreateCoupleRoomResponse> response = coupleService.createCoupleRoomWithOnboarding(providerId, request);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("커플룸 생성 API 오류: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("50001", "알 수 없는 서버 에러가 발생했습니다."));
        }
    }

    // 커플 매칭
    @PostMapping("/match")
    public ResponseEntity<ApiResponse<CoupleMatchResponse>> matchCouple(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid CoupleMatchRequest request) {
        
        try {
            // JWT에서 providerId 추출
            String providerId = userService.extractProviderId(userDetails);
            
            ApiResponse<CoupleMatchResponse> response = coupleService.matchCouple(providerId, request.inviteCode());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("커플 매칭 API 오류: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("50001", "알 수 없는 서버 에러가 발생했습니다."));
        }
    }

    // 커플 정보 변경 (경로 변수 없음)
    @PutMapping
    public ResponseEntity<ApiResponse<Void>> updateCoupleInfo(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid CoupleUpdateRequest request) {
        
        try {
            // JWT에서 providerId 추출
            String providerId = userService.extractProviderId(userDetails);
            
            ApiResponse<Void> response = coupleService.updateCoupleInfo(providerId, request);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("커플 정보 변경 API 오류: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("50001", "알 수 없는 서버 에러가 발생했습니다."));
        }
    }

    // 커플 정보 변경 (경로 변수 포함)
    @PutMapping("/{coupleId}")
    public ResponseEntity<ApiResponse<Void>> updateCoupleInfoById(
            @PathVariable String coupleId,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid CoupleUpdateRequest request) {
        
        try {
            // JWT에서 providerId 추출
            String providerId = userService.extractProviderId(userDetails);
            
            ApiResponse<Void> response = coupleService.updateCoupleInfo(providerId, request);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("커플 정보 변경 API 오류 (coupleId: {}): {}", coupleId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("50001", "알 수 없는 서버 에러가 발생했습니다."));
        }
    }

    // 커플 매칭 취소
    @DeleteMapping("/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelCouple(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request) {
        
        try {
            log.info("커플 매칭 취소 요청 시작");
            
            // JWT에서 coupleId 추출 시도
            String coupleId;
            try {
                coupleId = coupleService.getCoupleIdFromRequest(request);
                log.info("JWT에서 추출된 coupleId: {}", coupleId);
            } catch (Exception e) {
                log.warn("JWT에서 coupleId를 찾을 수 없음: {}", e.getMessage());
                
                // JWT에 coupleId가 없는 경우, 사용자의 커플 정보를 직접 조회
                String providerId = userService.extractProviderId(userDetails);
                coupleId = coupleService.getCoupleIdByProviderId(providerId);
                
                if (coupleId == null) {
                    return ResponseEntity.badRequest()
                            .body(ApiResponse.error("40001", "커플 정보를 찾을 수 없습니다. 먼저 커플 매칭을 진행해주세요."));
                }
                
                log.info("사용자 조회를 통해 찾은 coupleId: {}", coupleId);
            }
            
            ApiResponse<Void> response = coupleService.cancelCouple(coupleId);
            log.info("커플 매칭 취소 성공");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("커플 매칭 취소 API 오류: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("50001", "알 수 없는 서버 에러가 발생했습니다. (" + e.getMessage() + ")"));
        }
    }

    // Gateway용 티켓 정보 조회 API (DB에서 직접 조회)
    @GetMapping("/ticket")
    public ResponseEntity<TicketInfo> getCoupleTicket(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request) {
        
        try {
            log.info("Gateway용 티켓 정보 조회 요청 시작");
            
            // JWT에서 coupleId 추출 시도
            String coupleId;
            try {
                coupleId = coupleService.getCoupleIdFromRequest(request);
                log.info("JWT에서 추출된 coupleId: {}", coupleId);
            } catch (Exception e) {
                log.warn("JWT에서 coupleId를 찾을 수 없음: {}", e.getMessage());
                
                // JWT에 coupleId가 없는 경우, 사용자의 커플 정보를 직접 조회
                String providerId = userService.extractProviderId(userDetails);
                coupleId = coupleService.getCoupleIdByProviderId(providerId);
                
                if (coupleId == null) {
                    log.error("커플 정보를 찾을 수 없음 - providerId: {}", providerId);
                    return ResponseEntity.notFound().build();
                }
                
                log.info("사용자 조회를 통해 찾은 coupleId: {}", coupleId);
            }
            
            // DB에서 직접 티켓 정보 조회 (Redis 없이)
            TicketInfo ticketInfo = coupleService.getTicketInfoFromDb(coupleId);
            
            log.info("커플 티켓 정보 조회 성공 - coupleId: {}, ticket: {}", 
                    coupleId, ticketInfo.ticket());
            return ResponseEntity.ok(ticketInfo);
            
        } catch (Exception e) {
            log.error("커플 티켓 정보 조회 API 오류: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // AI 서버용 커플 추천 데이터 조회 API
    @GetMapping("/{coupleId}/recommendation-data")
    public ResponseEntity<ApiResponse<RecommendationDataResponse>> getRecommendationData(
            @PathVariable String coupleId,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request) {
        
        try {
            log.info("커플 추천 데이터 조회 요청 - coupleId: {}", coupleId);
            
            // JWT에서 coupleId 추출 시도
            String jwtCoupleId;
            try {
                jwtCoupleId = coupleService.getCoupleIdFromRequest(request);
                log.info("JWT에서 추출된 coupleId: {}", jwtCoupleId);
            } catch (Exception e) {
                log.warn("JWT에서 coupleId를 찾을 수 없음: {}", e.getMessage());
                
                // JWT에 coupleId가 없는 경우, 사용자의 커플 정보를 직접 조회
                String providerId = userService.extractProviderId(userDetails);
                jwtCoupleId = coupleService.getCoupleIdByProviderId(providerId);
                
                if (jwtCoupleId == null) {
                    return ResponseEntity.badRequest()
                            .body(ApiResponse.error("40001", "커플 정보를 찾을 수 없습니다. 먼저 커플 매칭을 진행해주세요."));
                }
                
                log.info("사용자 조회를 통해 찾은 coupleId: {}", jwtCoupleId);
            }
            
            // 경로 변수의 coupleId와 JWT의 coupleId가 일치하는지 확인
            if (!coupleId.equals(jwtCoupleId)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("40002", "접근 권한이 없습니다."));
            }
            
            ApiResponse<RecommendationDataResponse> response = coupleService.getRecommendationData(coupleId);
            
            if ("success".equals(response.status())) {
                log.info("커플 추천 데이터 조회 성공 - coupleId: {}", coupleId);
                return ResponseEntity.ok(response);
            } else {
                log.warn("커플 추천 데이터 조회 실패 - coupleId: {}, error: {}", coupleId, response.message());
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            log.error("커플 추천 데이터 조회 API 오류 - coupleId: {}, error: {}", coupleId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("50001", "알 수 없는 서버 에러가 발생했습니다. (" + e.getMessage() + ")"));
        }
    }

    // 티켓 정보 조회 (Gateway용)
    @GetMapping("/{coupleId}/ticket")
    public ResponseEntity<TicketInfo> getTicketInfo(@PathVariable String coupleId) {
        try {
            log.info("🎫 티켓 정보 조회 요청 - coupleId: {}", coupleId);
            
            TicketInfo ticketInfo = coupleService.getTicketInfoFromDb(coupleId);
            
            log.info("✅ 티켓 정보 조회 성공 - coupleId: {}, ticket: {}", 
                    coupleId, ticketInfo.ticket());
            
            return ResponseEntity.ok(ticketInfo);
            
        } catch (Exception e) {
            log.error("❌ 티켓 정보 조회 실패 - coupleId: {}, error: {}", 
                    coupleId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // 티켓 차감 (Gateway용)
    @PostMapping("/{coupleId}/ticket/consume")
    public ResponseEntity<ApiResponse<Boolean>> consumeTicket(@PathVariable String coupleId) {
        try {
            log.info("🎫 티켓 차감 요청 - coupleId: {}", coupleId);
            
            boolean success = coupleService.consumeTicket(coupleId);
            
            if (success) {
                log.info("✅ 티켓 차감 성공 - coupleId: {}", coupleId);
                return ResponseEntity.ok(ApiResponse.success("success", true));
            } else {
                log.warn("❌ 티켓 차감 실패 - 티켓 부족 - coupleId: {}", coupleId);
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("40002", "티켓이 부족합니다"));
            }
            
        } catch (Exception e) {
            log.error("❌ 티켓 차감 API 오류 - coupleId: {}, error: {}", 
                    coupleId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("50001", "티켓 차감 중 오류가 발생했습니다"));
        }
    }

    // 티켓 차감 및 Rock 완료 (init unlock용)
    @PostMapping("/{coupleId}/ticket/consume-and-complete")
    public ResponseEntity<ApiResponse<Boolean>> consumeTicketAndCompleteRock(@PathVariable String coupleId) {
        try {
            log.info("🎫 티켓 차감 및 Rock 완료 요청 - coupleId: {}", coupleId);
            
            boolean success = coupleService.consumeTicketAndCompleteRock(coupleId);
            
            if (success) {
                log.info("✅ 티켓 차감 및 Rock 완료 성공 - coupleId: {}", coupleId);
                return ResponseEntity.ok(ApiResponse.success("success", true));
            } else {
                log.warn("❌ 티켓 차감 및 Rock 완료 실패 - 티켓 부족 - coupleId: {}", coupleId);
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("40002", "티켓이 부족합니다"));
            }
            
        } catch (Exception e) {
            log.error("❌ 티켓 차감 및 Rock 완료 API 오류 - coupleId: {}, error: {}", 
                    coupleId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("50001", "티켓 차감 및 Rock 완료 중 오류가 발생했습니다"));
        }
    }
}


