package PitterPatter.loventure.authService.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import PitterPatter.loventure.authService.constants.RedirectStatus;
import PitterPatter.loventure.authService.dto.TicketInfo;
import PitterPatter.loventure.authService.dto.request.RockStatusCompleteRequest;
import PitterPatter.loventure.authService.dto.request.SignupRequest;
import PitterPatter.loventure.authService.dto.response.ApiResponse;
import PitterPatter.loventure.authService.dto.response.AuthResponse;
import PitterPatter.loventure.authService.dto.response.LogoutResponse;
import PitterPatter.loventure.authService.dto.response.MyPageApiResponse;
import PitterPatter.loventure.authService.dto.response.MyPageResponse;
import PitterPatter.loventure.authService.dto.response.ProfileUpdateResponse;
import PitterPatter.loventure.authService.dto.response.RockStatusCompleteResponse;
import PitterPatter.loventure.authService.dto.response.SignupResponse;
import PitterPatter.loventure.authService.dto.response.UserExistsResponse;
import PitterPatter.loventure.authService.dto.response.UserInfoApiResponse;
import PitterPatter.loventure.authService.dto.response.UserInfoResponse;
import PitterPatter.loventure.authService.dto.response.UserStatusResponse;
import PitterPatter.loventure.authService.exception.BusinessException;
import PitterPatter.loventure.authService.exception.ErrorCode;
import PitterPatter.loventure.authService.mapper.MyPageMapper;
import PitterPatter.loventure.authService.repository.CoupleRoom;
import PitterPatter.loventure.authService.repository.User;
import PitterPatter.loventure.authService.service.AuthService;
import PitterPatter.loventure.authService.service.CoupleService;
import PitterPatter.loventure.authService.service.TerritoryServiceClient;
import PitterPatter.loventure.authService.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final CoupleService coupleService;
    private final MyPageMapper myPageMapper;
    private final TerritoryServiceClient territoryServiceClient;

    @Value("${spring.jwt.redirect.onboarding}")
    private String onboardingRedirectUrl;

    @Value("${spring.jwt.redirect.coupleroom}")
    private String coupleroomRedirectUrl;

    @Value("${spring.jwt.redirect.home}")
    private String homeRedirectUrl;

    @Value("${spring.jwt.redirect.rock:https://lovuenture.us/home/district/choose}")
    private String rockRedirectUrl;

    /**
     * favicon.ico 요청 처리 (404 오류 방지)
     */
    @GetMapping("/favicon.ico")
    public ResponseEntity<Void> favicon() {
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * 토큰 갱신 (쿠키에서 refresh token 읽기)
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        try {
            log.info("Refresh token 요청 시작");
            log.info("요청 Origin: {}", request.getHeader("Origin"));
            log.info("요청 Referer: {}", request.getHeader("Referer"));
            log.info("요청 쿠키: {}", request.getCookies() != null ? java.util.Arrays.toString(request.getCookies()) : "쿠키 없음");
            
            // 쿠키 상세 정보 로깅
            if (request.getCookies() != null) {
                for (Cookie cookie : request.getCookies()) {
                    log.info("쿠키 이름: {}, 값: {}", cookie.getName(), cookie.getValue());
                }
            }
            
            // 쿠키에서 refresh token 추출
            String refreshToken = authService.getRefreshTokenFromCookie(request);
            log.info("쿠키에서 추출한 refresh token: {}", refreshToken != null ? "존재" : "없음");
            
            if (refreshToken == null) {
                log.warn("리프레시 토큰이 없습니다");
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("REFRESH_TOKEN_NOT_FOUND", "리프레시 토큰이 없습니다"));
            }

            AuthResponse authResponse = authService.refreshToken(refreshToken);
            log.info("토큰 갱신 결과: {}", authResponse.success() ? "성공" : "실패");
            
            if (authResponse.success()) {
                // 새로운 refresh token을 쿠키에 저장
                authService.setRefreshTokenCookie(response, authResponse.refreshToken());
                log.info("새로운 refresh token 쿠키 저장 완료");
                return ResponseEntity.ok(authResponse);
            } else {
                log.warn("토큰 갱신 실패: {}", authResponse.message());
                return ResponseEntity.badRequest().body(authResponse);
            }
        } catch (Exception e) {
            log.error("토큰 갱신 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("REFRESH_TOKEN_ERROR", "토큰 갱신 중 오류가 발생했습니다"));
        }
    }


    /**
     * 사용자 프로필 상세 조회 (마이페이지용)
     * @AuthenticationPrincipal을 사용하여 JWTFilter가 검증하고 SecurityContext에 저장한
     * 인증된 사용자 정보를 직접 주입받습니다.
     */
    @GetMapping("/mypage")
    public ResponseEntity<?> getMyPage(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            if (userDetails == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("UNAUTHORIZED", "인증된 사용자 정보를 찾을 수 없습니다"));
            }

            String providerId = userDetails.getUsername();
            User user = userService.validateUserByProviderId(providerId);

            // 커플 정보 조회
            Optional<CoupleRoom> coupleRoomOpt = coupleService.getCoupleInfo(providerId);
            User partner = null;
            
            if (coupleRoomOpt.isPresent()) {
                CoupleRoom coupleRoom = coupleRoomOpt.get();
                // 파트너 정보 조회 (파트너가 존재하는 경우에만)
                String partnerProviderId = coupleRoom.getCreatorUserId().equals(providerId) 
                    ? coupleRoom.getPartnerUserId() 
                    : coupleRoom.getCreatorUserId();
                
                if (partnerProviderId != null) {
                    try {
                        partner = userService.validateUserByProviderId(partnerProviderId);
                    } catch (BusinessException e) {
                        log.warn("파트너 사용자 정보를 찾을 수 없습니다: {}", partnerProviderId);
                        // 파트너 정보가 없어도 커플룸 정보는 표시
                    }
                }
            }

            // 티켓 정보 조회 (커플이 있는 경우에만)
            Integer ticket = null;
            if (coupleRoomOpt.isPresent()) {
                try {
                    String coupleId = coupleRoomOpt.get().getCoupleId();
                    if (coupleId != null) {
                        TicketInfo ticketInfo = coupleService.getTicketInfoFromDb(coupleId);
                        ticket = ticketInfo.ticket();
                    }
                } catch (Exception e) {
                    log.warn("티켓 정보 조회 실패: {}", e.getMessage());
                    // 티켓 정보 조회 실패해도 마이페이지는 정상 응답
                }
            }

            // 매퍼를 사용하여 MyPageResponse 생성
            MyPageResponse myPageResponse = myPageMapper.toMyPageResponse(user, coupleRoomOpt, partner, ticket);

            MyPageApiResponse response = new MyPageApiResponse(true, myPageResponse);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("마이페이지 정보 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("MYPAGE_ERROR", "마이페이지 정보 조회 중 오류가 발생했습니다"));
        }
    }

    /**
     * 사용자 프로필 수정
     */
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@AuthenticationPrincipal UserDetails userDetails,
                                         @RequestBody @Valid PitterPatter.loventure.authService.dto.request.ProfileUpdateRequest request) {
        try {
            if (userDetails == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("UNAUTHORIZED", "인증된 사용자 정보를 찾을 수 없습니다"));
            }

            String providerId = userDetails.getUsername();
            User updatedUser = userService.updateProfile(providerId, request);

            ProfileUpdateResponse response = new ProfileUpdateResponse(
                true,
                "프로필이 성공적으로 수정되었습니다",
                updatedUser.getUserId()
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("프로필 수정 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("PROFILE_UPDATE_ERROR", "프로필 수정 중 오류가 발생했습니다"));
        }
    }

    /**
     * 로그아웃
     * 클라이언트는 이 API 호출 후 자체적으로 토큰을 삭제해야 합니다.
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization") String authorization,
                                    HttpServletResponse response) {
        try {
            if (authorization != null && authorization.startsWith("Bearer ")) {
                // String token = authorization.substring(7);
                log.info("로그아웃 요청 처리 완료. 클라이언트는 토큰을 폐기해야 합니다.");
            }

            // Refresh token 쿠키 삭제
            authService.clearRefreshTokenCookie(response);

            LogoutResponse logoutResponse = new LogoutResponse(
                true,
                "로그아웃되었습니다. 클라이언트에서 토큰을 삭제해주세요."
            );
            return ResponseEntity.ok(logoutResponse);

        } catch (Exception e) {
            log.error("로그아웃 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("LOGOUT_ERROR", "로그아웃 중 오류가 발생했습니다"));
        }
    }


    /**
     * 계정 상태 확인
     */
    @GetMapping("/status")
    public ResponseEntity<?> checkAccountStatus(@RequestParam String email) {
        try {
            User user = userService.getUserByEmail(email);

            if (user == null) {
                return ResponseEntity.ok(ApiResponse.success("등록되지 않은 이메일입니다", 
                    Map.of("exists", false)));
            }

            UserExistsResponse response = new UserExistsResponse(
                true,
                user.getStatus(),
                user.getProviderType()
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("계정 상태 확인 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("ACCOUNT_STATUS_ERROR", "계정 상태 확인 중 오류가 발생했습니다"));
        }
    }



    /**
     * 회원가입 (명세서: POST /api/auth/signup)
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@RequestBody SignupRequest signupRequest) {
        try {
            // 필수 입력값 검증
            if (signupRequest.getEmail() == null || signupRequest.getName() == null ||
                    signupRequest.getProviderId() == null || signupRequest.getProviderType() == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("40001", "필수 입력 코드가 누락되었습니다"));
            }

            // 이메일 중복 체크
            User existingUser = userService.getUserByEmail(signupRequest.getEmail());
            if (existingUser != null) {
                return ResponseEntity.status(409)
                        .body(ApiResponse.error("40901", "이미 존재하는 회원입니다."));
            }

            // providerId 중복 체크
            User existingProviderUser = userService.getUserByProviderId(signupRequest.getProviderId());
            if (existingProviderUser != null) {
                return ResponseEntity.status(409)
                        .body(ApiResponse.error("40901", "이미 존재하는 회원입니다."));
            }

            // 신규 사용자 생성
            User savedUser = userService.createUser(signupRequest);

            SignupResponse signupResponse = new SignupResponse(
                    savedUser.getUserId(),
                    savedUser.getCreatedAt(),
                    savedUser.getUpdatedAt()
            );

            return ResponseEntity.ok(ApiResponse.success(signupResponse));

        } catch (Exception e) {
            log.error("회원가입 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("50001", "알 수 없는 서버 에러가 발생했습니다.(" + e.getMessage() + ")"));
        }
    }


    /**
     * 유저 정보 조회 API (다른 서비스에서 사용)
     * GET /api/auth/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserInfo(@PathVariable String userId) {
        try {
            log.info("유저 정보 조회 요청 - userId: {}", userId);
            
            // userId로 사용자 조회
            User user = userService.getUserById(userId);
            
            // UserInfoResponse 생성
            UserInfoResponse userInfoResponse = new UserInfoResponse(
                user.getUserId(),
                user.getName()
            );
            
            // API 명세서에 맞는 응답 형식
            UserInfoApiResponse response = new UserInfoApiResponse("success", userInfoResponse);
            
            log.info("유저 정보 조회 성공 - userId: {}, name: {}", userId, user.getName());
            return ResponseEntity.ok(response);
            
        } catch (BusinessException e) {
            log.warn("유저 정보 조회 실패 - userId: {}, error: {}", userId, e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", e.getErrorCode().getCode());
            errorResponse.put("message", e.getErrorCode().getMessage());
            
            if (e.getErrorCode() == ErrorCode.USER_NOT_FOUND || e.getErrorCode() == ErrorCode.USER_NOT_FOUND_BY_ID) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            } else if (e.getErrorCode() == ErrorCode.INVALID_USER_ID_FORMAT) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
            
        } catch (Exception e) {
            log.error("유저 정보 조회 중 오류 발생 - userId: {}, error: {}", userId, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "50001");
            errorResponse.put("message", "알 수 없는 서버 에러가 발생했습니다.(" + e.getMessage() + ")");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 사용자 상태별 리다이렉트 URL 반환
     * - 신규회원 또는 개인 온보딩 미완료: /onboarding
     * - 개인 온보딩 완료, 커플 매칭 미완료: /coupleroom
     * - 커플 매칭 완료, rock 미완료: /home/district/choose
     * - 개인 온보딩, 커플 매칭, rock 모두 완료: /home
     */
    @GetMapping("/redirect")
    public ResponseEntity<?> getUserRedirectUrl(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            if (userDetails == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("UNAUTHORIZED", "인증된 사용자 정보를 찾을 수 없습니다"));
            }

            String providerId = userDetails.getUsername();
            User user = userService.validateUserByProviderId(providerId);

            // 개인 온보딩 완료 여부 확인
            boolean isOnboardingCompleted = userService.isOnboardingCompleted(user);
            
            // 커플 매칭 상태 확인
            boolean isCoupled = coupleService.isUserCoupled(providerId);
            
            // rock 완료 여부 확인
            boolean isRockCompleted = userService.isRockCompleted(user);

            String redirectUrl;
            String status;

            if (!isOnboardingCompleted) {
                // 신규회원 또는 개인 온보딩 미완료
                redirectUrl = onboardingRedirectUrl;
                status = RedirectStatus.ONBOARDING_REQUIRED;
            } else if (!isCoupled) {
                // 개인 온보딩 완료, 커플 매칭 미완료
                redirectUrl = coupleroomRedirectUrl;
                status = RedirectStatus.COUPLE_MATCHING_REQUIRED;
            } else if (!isRockCompleted) {
                // 커플 매칭 완료, rock 미완료
                redirectUrl = rockRedirectUrl;
                status = RedirectStatus.ROCK_REQUIRED;
            } else {
                // 개인 온보딩, 커플 매칭, rock 모두 완료
                redirectUrl = homeRedirectUrl;
                status = RedirectStatus.COMPLETED;
            }

            UserStatusResponse response = new UserStatusResponse(
                true,
                redirectUrl,
                status,
                isOnboardingCompleted,
                isCoupled,
                isRockCompleted
            );

            log.info("사용자 리다이렉트 URL 반환 - userId: {}, status: {}, redirectUrl: {}", 
                    user.getUserId(), status, redirectUrl);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("사용자 리다이렉트 URL 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("REDIRECT_URL_ERROR", "사용자 리다이렉트 URL 조회 중 오류가 발생했습니다"));
        }
    }


    /**
     * Territory-service로부터 region 상태 완료 요청을 받는 API
     */
    @PostMapping("/api/region/status")
    public ResponseEntity<?> completeRockStatus(
            @RequestBody RockStatusCompleteRequest request) {
        try {
            log.info("Rock 상태 완료 요청 수신 - coupleId: {}, userId: {}", 
                    request.coupleId(), request.userId());
            
            // 1. 커플룸 기반으로 상태 변경 (대기 중인 사용자 포함)
            coupleService.completeRockStatusForCouple(request.coupleId());
            
            // 2. Territory-service로 ACK 전송
            territoryServiceClient.sendRockCompletionAck(
                request.coupleId(), 
                request.userId()
            );
            
            RockStatusCompleteResponse response = new RockStatusCompleteResponse(
                true,
                "Rock 상태 변경 완료",
                request.coupleId(),
                request.userId()
            );
            
            log.info("Rock 상태 변경 완료 - coupleId: {}, userId: {}", 
                    request.coupleId(), request.userId());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Rock 상태 변경 실패 - coupleId: {}, userId: {}, error: {}", 
                    request.coupleId(), request.userId(), e.getMessage(), e);
            
            RockStatusCompleteResponse errorResponse = new RockStatusCompleteResponse(
                false,
                "Rock 상태 변경 실패: " + e.getMessage(),
                request.coupleId(),
                request.userId()
            );
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
