package PitterPatter.loventure.authService.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import PitterPatter.loventure.authService.dto.response.UserInfoResponse;
import PitterPatter.loventure.authService.exception.BusinessException;
import PitterPatter.loventure.authService.exception.ErrorCode;
import PitterPatter.loventure.authService.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * internal/user/ 경로에 대한 요청을 처리하는 컨트롤러
 * MSA 내부 통신을 위한 사용자 정보 조회 API를 제공합니다.
 */
@RestController
@RequestMapping("/internal/user")
@RequiredArgsConstructor
@Slf4j
public class InternalUserController {

    private final UserService userService;

    /**
     * internal/user/{userId} 경로에 대한 요청 처리
     * Content 서비스에서 사용자 정보를 조회하기 위한 내부 API
     * 
     * @param userId 사용자 ID
     * @return 사용자 정보 또는 404 Not Found
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserInfoResponse> getUserById(@PathVariable String userId) {
        try {
            log.info("내부 MSA 통신: 사용자 정보 조회 요청 - userId: {}", userId);
            
            // userId로 사용자 조회
            var user = userService.getUserById(userId);
            
            // UserInfoResponse 생성
            UserInfoResponse userInfoResponse = new UserInfoResponse(
                user.getUserId().toString(),
                user.getName()
            );
            
            log.info("내부 MSA 통신: 사용자 정보 조회 성공 - userId: {}, name: {}", userId, user.getName());
            return ResponseEntity.ok(userInfoResponse);
            
        } catch (BusinessException e) {
            log.warn("내부 MSA 통신: 사용자 정보 조회 실패 - userId: {}, error: {}", userId, e.getMessage());
            
            if (e.getErrorCode() == ErrorCode.USER_NOT_FOUND || e.getErrorCode() == ErrorCode.USER_NOT_FOUND_BY_ID) {
                return ResponseEntity.notFound().build();
            } else {
                return ResponseEntity.internalServerError().build();
            }
            
        } catch (Exception e) {
            log.error("내부 MSA 통신: 사용자 정보 조회 중 오류 발생 - userId: {}, error: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
