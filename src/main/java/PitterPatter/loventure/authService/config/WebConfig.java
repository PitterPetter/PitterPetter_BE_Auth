package PitterPatter.loventure.authService.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 웹 MVC 설정 클래스
 * 정적 리소스 핸들러 설정을 담당합니다.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 정적 리소스 핸들러 설정
     * internal/user/ 경로에 대한 요청을 처리합니다.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // internal/user/ 경로에 대한 정적 리소스 핸들러 추가
        registry.addResourceHandler("/internal/user/**")
                .addResourceLocations("classpath:/static/internal/user/")
                .setCachePeriod(3600); // 1시간 캐시
    }
}
