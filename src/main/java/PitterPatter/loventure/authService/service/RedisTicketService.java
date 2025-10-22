package PitterPatter.loventure.authService.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis 기반 티켓 관리 서비스
 * - 커플의 티켓 정보를 Redis에 캐시
 * - DB와 Redis 동기화
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisTicketService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String TICKET_KEY_PREFIX = "couple:ticket:";
    private static final Duration TICKET_CACHE_TTL = Duration.ofHours(24); // 24시간 캐시
    
    /**
     * Redis에 티켓 개수 저장
     */
    public void setTicketCount(String coupleId, int ticketCount) {
        try {
            String key = TICKET_KEY_PREFIX + coupleId;
            redisTemplate.opsForValue().set(key, ticketCount, TICKET_CACHE_TTL);
            log.info("🎟️ Redis에 티켓 정보 저장 - coupleId: {}, ticketCount: {}", coupleId, ticketCount);
        } catch (Exception e) {
            log.error("❌ Redis 티켓 저장 실패 - coupleId: {}, error: {}", coupleId, e.getMessage());
        }
    }
    
    /**
     * Redis에서 티켓 개수 조회
     */
    public Integer getTicketCount(String coupleId) {
        try {
            String key = TICKET_KEY_PREFIX + coupleId;
            Object ticketCount = redisTemplate.opsForValue().get(key);
            if (ticketCount instanceof Integer) {
                log.debug("🎟️ Redis에서 티켓 정보 조회 - coupleId: {}, ticketCount: {}", coupleId, ticketCount);
                return (Integer) ticketCount;
            }
            return null;
        } catch (Exception e) {
            log.error("❌ Redis 티켓 조회 실패 - coupleId: {}, error: {}", coupleId, e.getMessage());
            return null;
        }
    }
    
    /**
     * Redis에서 티켓 차감
     */
    public boolean consumeTicket(String coupleId) {
        try {
            String key = TICKET_KEY_PREFIX + coupleId;
            Long result = redisTemplate.opsForValue().decrement(key);
            if (result != null && result >= 0) {
                log.info("🎟️ Redis에서 티켓 차감 성공 - coupleId: {}, 남은 티켓: {}", coupleId, result);
                return true;
            } else {
                log.warn("❌ Redis에서 티켓 차감 실패 - 티켓 부족 - coupleId: {}, result: {}", coupleId, result);
                return false;
            }
        } catch (Exception e) {
            log.error("❌ Redis 티켓 차감 실패 - coupleId: {}, error: {}", coupleId, e.getMessage());
            return false;
        }
    }
    
    /**
     * Redis에서 티켓 정보 삭제
     */
    public void deleteTicketCount(String coupleId) {
        try {
            String key = TICKET_KEY_PREFIX + coupleId;
            redisTemplate.delete(key);
            log.info("🗑️ Redis에서 티켓 정보 삭제 - coupleId: {}", coupleId);
        } catch (Exception e) {
            log.error("❌ Redis 티켓 삭제 실패 - coupleId: {}, error: {}", coupleId, e.getMessage());
        }
    }
}
