package PitterPatter.loventure.authService.listener;

import java.util.Map;

import org.springframework.stereotype.Component;

import PitterPatter.loventure.authService.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Redis Write-Through 패턴용 티켓 동기화 리스너
 * 
 * 실제 구현은 Gateway에서 해야 합니다.
 * Gateway에서 Redis에 티켓 정보를 쓸 때 Redis Pub/Sub으로 이벤트를 발행하고,
 * Auth Service에서 해당 이벤트를 구독하여 DB에 자동 동기화합니다.
 * 
 * Gateway 구현 예시:
 * ```java
 * // Gateway에서 Redis 쓰기 시
 * redisTemplate.opsForValue().set(key, ticketData);
 * 
 * // Redis Pub/Sub으로 동기화 이벤트 발행
 * Map<String, Object> event = Map.of(
 *     "coupleId", coupleId,
 *     "ticketData", ticketData,
 *     "timestamp", System.currentTimeMillis(),
 *     "source", "gateway"
 * );
 * redisTemplate.convertAndSend("ticket-sync-channel", event);
 * ```
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TicketSyncListener {
    
    private final TicketService ticketService;
    
    /**
     * Redis Pub/Sub에서 받은 티켓 동기화 이벤트 처리
     * 실제 구현 시 Gateway에서 Redis Pub/Sub 이벤트를 구독하여 호출
     */
    @SuppressWarnings("unchecked")
    public void handleSyncEvent(Object message) {
        try {
            if (message instanceof Map<?, ?> event) {
                String coupleId = String.valueOf(event.get("coupleId"));
                Object ticketData = event.get("ticketData");
                String source = String.valueOf(event.get("source"));
                
                log.info("📨 티켓 동기화 이벤트 수신 - coupleId: {}, source: {}", coupleId, source);
                
                // Auth Service DB 업데이트
                ticketService.updateTicketInfoFromGateway(coupleId, ticketData);
                
                log.info("✅ 티켓 동기화 이벤트 처리 완료 - coupleId: {}", coupleId);
            } else {
                log.warn("⚠️ 지원하지 않는 메시지 타입: {}", message != null ? message.getClass() : "null");
            }
            
        } catch (Exception e) {
            log.error("🚨 티켓 동기화 이벤트 처리 실패 - message: {}, error: {}", message, e.getMessage(), e);
        }
    }
}
