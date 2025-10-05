package PitterPatter.loventure.authService.controller;

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
import PitterPatter.loventure.authService.exception.BusinessException;
import PitterPatter.loventure.authService.exception.ErrorCode;
import PitterPatter.loventure.authService.mapper.UserMapper;
import PitterPatter.loventure.authService.repository.User;
import PitterPatter.loventure.authService.service.RecommendationDataService;
import PitterPatter.loventure.authService.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final RecommendationDataService recommendationDataService;
    private final UserService userService;
    private final UserMapper userMapper;

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
                        .body(ApiResponse.error(ErrorCode.NO_PERMISSION.getCode(), "권한이 없는 사용자 입니다."));
            }

            // 삭제할 사용자 조회
            User targetUser = userService.getUserById(userId);

            // 본인 계정인지 확인 (TSID 비교)
            if (!currentUser.getUserId().equals(targetUser.getUserId())) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error(ErrorCode.NO_PERMISSION.getCode(), "탈퇴 권한이 없습니다.(본인 계정 아닐 때)"));
            }

            // 사용자 삭제 (소프트 삭제)
            userService.deleteUser(targetUser);

            DeleteUserResponse deleteResponse = userMapper.toDeleteUserResponse();

            return ResponseEntity.ok(ApiResponse.success(deleteResponse));

        } catch (BusinessException e) {
            log.warn("회원 삭제 중 비즈니스 오류 발생: {}", e.getMessage());
            return ResponseEntity.status(400)
                    .body(ApiResponse.error(e.getErrorCode().getCode(), e.getMessage()));
        } catch (Exception e) {
            log.error("회원 삭제 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR.getCode(), "알 수 없는 서버 에러가 발생했습니다."));
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
            RecommendationDataResponse responseDto = recommendationDataService.getRecommendationDataByUserId(userId);

            return ResponseEntity.ok(ApiResponse.success(responseDto));

        } catch (BusinessException e) {
            // Service에서 발생한 비즈니스 예외를 적절한 HTTP 상태 코드로 매핑
            log.warn("추천 데이터 조회 중 비즈니스 오류 발생: {}", e.getMessage());
            return ResponseEntity.status(400)
                    .body(ApiResponse.error(e.getErrorCode().getCode(), e.getMessage()));

        } catch (Exception e) {
            log.error("추천 데이터 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR.getCode(), "알 수 없는 서버 에러가 발생했습니다."));
        }
    }
}