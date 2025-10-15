package PitterPatter.loventure.authService.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

/**
 * internal/user/ 경로에 대한 요청을 처리하는 컨트롤러
 * 정적 리소스가 없을 때 적절한 응답을 제공합니다.
 */
@RestController
@RequestMapping("/internal/user")
@Slf4j
public class InternalUserController {

    /**
     * internal/user/{userId} 경로에 대한 요청 처리
     * 현재는 사용자 ID를 로그로 기록하고 404 응답을 반환합니다.
     * 
     * @param userId 사용자 ID
     * @return 404 Not Found 응답
     */
    @GetMapping("/{userId}")
    public ResponseEntity<Void> handleInternalUserRequest(@PathVariable String userId) {
        log.info("internal/user/{} 경로에 대한 요청이 들어왔습니다. 정적 리소스가 없어 404를 반환합니다.", userId);
        
        // TODO: 필요에 따라 사용자별 정적 리소스 처리 로직을 구현할 수 있습니다.
        // 예: 사용자 프로필 이미지, 개인화된 리소스 등
        
        return ResponseEntity.notFound().build();
    }
}
