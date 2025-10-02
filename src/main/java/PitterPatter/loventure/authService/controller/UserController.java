package PitterPatter.loventure.authService.controller;

import java.math.BigInteger;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import PitterPatter.loventure.authService.dto.response.ApiResponse;
import PitterPatter.loventure.authService.dto.response.DeleteUserResponse;
import PitterPatter.loventure.authService.dto.response.RecommendationDataResponse;
import PitterPatter.loventure.authService.repository.User;
import PitterPatter.loventure.authService.repository.UserRepository;
import PitterPatter.loventure.authService.service.RecommendationDataService;
import PitterPatter.loventure.authService.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserRepository userRepository;
    private final RecommendationDataService recommendationDataService;
    private final UserService userService;

    /**
     * 회원 삭제 (명세서: DELETE /api/users/{userId})
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<DeleteUserResponse>> deleteUser(
            @PathVariable String userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            // 현재 로그인한 사용자 정보 조회
            User currentUser = userService.getUserFromUserDetails(userDetails);
            
            if (currentUser == null) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error("40102", "권한이 없는 사용자 입니다."));
            }

            // 삭제할 사용자 조회 (BigInteger userId로 조회)
            BigInteger targetUserId;
            try {
                targetUserId = new BigInteger(userId);
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("40004", "잘못된 사용자 ID 형식입니다."));
            }

            User targetUser = userRepository.findById(targetUserId)
                    .orElse(null);
            if (targetUser == null) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error("40401", "존재하지 않는 회원입니다."));
            }


            // 본인 계정인지 확인 (TSID 비교)
            if (!currentUser.getUserId().equals(targetUser.getUserId())) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error("40102", "탈퇴 권한이 없습니다.(본인 계정 아닐 때)"));
            }

            // 사용자 삭제 (소프트 삭제 - 상태를 DEACTIVATED로 변경)
            targetUser.setStatus(PitterPatter.loventure.authService.repository.AccountStatus.DEACTIVATED);
            userRepository.save(targetUser);

            DeleteUserResponse deleteResponse = new DeleteUserResponse(
                    "success",
                    "회원 탈퇴 완료"
            );

            return ResponseEntity.ok(ApiResponse.success(deleteResponse));

        } catch (Exception e) {
            log.error("회원 삭제 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("50001", "알 수 없는 서버 에러가 발생했습니다.(" + e.getMessage() + ")"));
        }
    }

    /**
     * 코스 추천을 위한 사용자 및 커플 온보딩 정보 조회 (For AI Service)
     * 명세서: GET /api/users/recommendation-data/{userId}
     */
    @GetMapping("/recommendation-data/{userId}")
    public ResponseEntity<ApiResponse<RecommendationDataResponse>> getRecommendationData(
            @PathVariable String userId) {
        try {
            // 서비스 로직 호출
            RecommendationDataResponse responseDto = recommendationDataService.getRecommendationData(userId);

            return ResponseEntity.ok(ApiResponse.success(responseDto));

        } catch (RuntimeException e) {
            // Service에서 발생한 사용자 미발견 예외를 404로 매핑
            if (e.getMessage().contains("User not found")) {
                log.warn("요청된 userId에 해당하는 회원을 찾을 수 없습니다: {}", userId);
                return ResponseEntity.status(404)
                        .body(ApiResponse.error("40402", "존재하지 않는 회원(userId)입니다."));
            }

            log.error("추천 데이터 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("50002", "알 수 없는 서버 에러가 발생했습니다.(" + e.getMessage() + ")"));
        }
    }
}