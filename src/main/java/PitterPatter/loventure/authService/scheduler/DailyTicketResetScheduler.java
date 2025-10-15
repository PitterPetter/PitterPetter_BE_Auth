package PitterPatter.loventure.authService.scheduler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import PitterPatter.loventure.authService.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 일일 티켓 초기화 스케줄러
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class DailyTicketResetScheduler {

    private final TicketService ticketService;

    @Value("${ticket.daily-reset.enabled:true}")
    private boolean resetEnabled;

    /**
     * 매일 정각(00:00:00)에 일일 티켓 초기화 실행
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void resetDailyTickets() {
        if (!resetEnabled) {
            log.info("⏰ 일일 티켓 초기화가 비활성화되어 있습니다");
            return;
        }

        String currentTime = java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        log.info("🕛 일일 티켓 초기화 시작 - 시간: {}", currentTime);

        try {
            ticketService.resetAllDailyTickets();
            log.info("🎉 일일 티켓 초기화 성공 - 시간: {}", currentTime);
        } catch (Exception e) {
            log.error("❌ 일일 티켓 초기화 실패 - 시간: {}, 오류: {}", currentTime, e.getMessage());
        }
    }
}
