package PitterPatter.loventure.authService.controller;

import PitterPatter.loventure.authService.dto.response.ApiResponse;
import PitterPatter.loventure.authService.dto.response.DeleteUserResponse;
import PitterPatter.loventure.authService.repository.User;
import PitterPatter.loventure.authService.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;


@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserRepository userRepository;


    /**
     * 회원 삭제 (명세서: DELETE /api/users/{userId})
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<DeleteUserResponse>> deleteUser(
            @PathVariable String userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            // 현재 로그인한 사용자 정보 조회
            String currentProviderId = userDetails.getUsername();
            User currentUser = userRepository.findByProviderId(currentProviderId);
            
            if (currentUser == null) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error("40102", "권한이 없는 사용자 입니다."));
            }

            // 삭제할 사용자 조회 (TSID로 조회)
            Long targetUserId;
            try {
                targetUserId = Long.parseLong(userId);
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("40004", "잘못된 사용자 ID 형식입니다."));
            }

            Optional<User> targetUserOpt = userRepository.findById(targetUserId);
            if (targetUserOpt.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error("40401", "존재하지 않는 회원입니다."));
            }

            User targetUser = targetUserOpt.get();

            // 본인 계정인지 확인 (TSID 비교)
            if (!currentUser.getTsid().equals(targetUser.getTsid())) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error("40102", "탈퇴 권한이 없습니다.(본인 계정 아닐 때)"));
            }

            // 사용자 삭제 (소프트 삭제 - 상태를 DEACTIVATED로 변경)
            targetUser.setStatus(PitterPatter.loventure.authService.repository.AccountStatus.DEACTIVATED);
            userRepository.save(targetUser);

            DeleteUserResponse deleteResponse = DeleteUserResponse.builder()
                    .status("success")
                    .message("회원 탈퇴 완료")
                    .build();

            return ResponseEntity.ok(ApiResponse.success(deleteResponse));

        } catch (Exception e) {
            log.error("회원 삭제 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("50001", "알 수 없는 서버 에러가 발생했습니다.(" + e.getMessage() + ")"));
        }
    }
}