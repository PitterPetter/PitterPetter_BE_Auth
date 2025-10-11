package PitterPatter.loventure.authService.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * favicon.ico 요청을 처리하는 컨트롤러
 * 브라우저가 자동으로 요청하는 favicon.ico에 대한 404 오류를 방지합니다.
 */
@RestController
public class FaviconController {

    /**
     * 루트 경로의 favicon.ico 요청 처리
     * 204 No Content 응답으로 브라우저의 favicon 요청을 처리합니다.
     */
    @GetMapping("/favicon.ico")
    public ResponseEntity<Void> favicon() {
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
