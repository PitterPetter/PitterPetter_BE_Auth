package PitterPatter.loventure.authService.scheduler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import PitterPatter.loventure.authService.repository.CoupleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 일일 티켓 초기화 스케줄러
 * 매일 자정에 isTodayTicket을 true로 초기화
 */
@Slf4j
@Component
@RequiredArgsConstructor
@EnableScheduling
public class DailyTicketResetScheduler {
    
    private final CoupleRepository coupleRepository;
    
    /**
     * 매일 자정(00:00:00)에 isTodayTicket 초기화
     * cron = "0 0 0 * * *" = 초 분 시 일 월 요일
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void resetDailyTickets() {
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        log.info("🕛 일일 티켓 초기화 시작 - 시간: {}", currentTime);
        
        try {
            LocalDateTime now = LocalDateTime.now();
            int updatedCount = coupleRepository.resetAllDailyTickets(now);
            
            log.info("✅ 일일 티켓 초기화 완료 - 시간: {}, 업데이트된 커플 수: {}", currentTime, updatedCount);
            
        } catch (Exception e) {
            log.error("❌ 일일 티켓 초기화 실패 - 시간: {}, 오류: {}", currentTime, e.getMessage(), e);
        }
    }
    
    /**
     * 매일 자정 1분 후에 초기화 상태 확인
     */
    @Scheduled(cron = "0 1 0 * * *")
    public void verifyResetStatus() {
        try {
            long totalCouples = coupleRepository.count();
            long availableTickets = coupleRepository.countByIsTodayTicketTrue();
            
            log.info("📊 일일 티켓 초기화 상태 확인:");
            log.info("   - 전체 커플 수: {}", totalCouples);
            log.info("   - 오늘 티켓 사용 가능한 커플 수: {}", availableTickets);
            
            if (availableTickets == totalCouples) {
                log.info("✅ 일일 티켓 초기화 상태 정상 - 모든 커플이 오늘 티켓 사용 가능");
            } else {
                log.warn("⚠️ 일일 티켓 초기화 상태 이상 - 일부 커플만 오늘 티켓 사용 가능");
            }
            
        } catch (Exception e) {
            log.error("❌ 일일 티켓 초기화 상태 확인 실패: {}", e.getMessage());
        }
    }
}
