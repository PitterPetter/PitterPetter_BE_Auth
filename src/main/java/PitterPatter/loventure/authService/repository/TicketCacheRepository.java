package PitterPatter.loventure.authService.repository;

import java.util.Set;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import PitterPatter.loventure.authService.dto.TicketInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 티켓 정보 Redis 캐시 Repository
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class TicketCacheRepository {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String COUPLE_TICKET_KEY_PREFIX = "coupleId:";
    private static final int DEFAULT_TTL_SECONDS = 7 * 24 * 60 * 60; // 7일

    /**
     * 티켓 정보 저장
     */
    public void save(String coupleId, TicketInfo ticketInfo) {
        try {
            String key = COUPLE_TICKET_KEY_PREFIX + coupleId;
            redisTemplate.opsForValue().set(key, ticketInfo, DEFAULT_TTL_SECONDS);
            log.debug("Redis 저장 성공 - Key: {}, Value: {}", key, ticketInfo);
        } catch (Exception e) {
            log.error("Redis 저장 실패 - Key: {}, Value: {}, Error: {}", 
                COUPLE_TICKET_KEY_PREFIX + coupleId, ticketInfo, e.getMessage(), e);
            throw new RuntimeException("Redis 데이터 저장 실패", e);
        }
    }

    /**
     * 티켓 정보 조회
     */
    public TicketInfo findByCoupleId(String coupleId) {
        try {
            String key = COUPLE_TICKET_KEY_PREFIX + coupleId;
            Object value = redisTemplate.opsForValue().get(key);
            log.debug("Redis 조회 - Key: {}, Value: {}", key, value);
            
            if (value instanceof TicketInfo ticketInfo) {
                return ticketInfo;
            }
            return null;
        } catch (Exception e) {
            log.error("Redis 조회 실패 - Key: {}, Error: {}", 
                COUPLE_TICKET_KEY_PREFIX + coupleId, e.getMessage(), e);
            throw new RuntimeException("Redis 데이터 조회 실패", e);
        }
    }

    /**
     * 티켓 정보 삭제
     */
    public void deleteByCoupleId(String coupleId) {
        try {
            String key = COUPLE_TICKET_KEY_PREFIX + coupleId;
            redisTemplate.delete(key);
            log.debug("Redis 삭제 - Key: {}", key);
        } catch (Exception e) {
            log.error("Redis 삭제 실패 - Key: {}, Error: {}", 
                COUPLE_TICKET_KEY_PREFIX + coupleId, e.getMessage(), e);
            throw new RuntimeException("Redis 데이터 삭제 실패", e);
        }
    }

    /**
     * 모든 티켓 키 조회
     */
    public Set<String> findAllKeys() {
        try {
            return redisTemplate.keys(COUPLE_TICKET_KEY_PREFIX + "*");
        } catch (Exception e) {
            log.error("Redis 키 조회 실패 - Error: {}", e.getMessage(), e);
            throw new RuntimeException("Redis 키 조회 실패", e);
        }
    }

    /**
     * 일일 티켓 초기화 (모든 티켓의 isTodayTicket을 true로 변경)
     */
    public void resetAllDailyTickets() {
        try {
            Set<String> keys = findAllKeys();
            for (String key : keys) {
                TicketInfo ticketInfo = findByCoupleId(key.replace(COUPLE_TICKET_KEY_PREFIX, ""));
                if (ticketInfo != null) {
                    TicketInfo resetTicketInfo = new TicketInfo(
                        ticketInfo.coupleId(),
                        ticketInfo.ticket(),
                        true, // isTodayTicket을 true로 변경
                        java.time.LocalDateTime.now()
                    );
                    save(ticketInfo.coupleId(), resetTicketInfo);
                }
            }
            log.info("Redis 일일 티켓 초기화 완료 - 처리된 키 수: {}", keys.size());
        } catch (Exception e) {
            log.error("Redis 일일 티켓 초기화 실패 - Error: {}", e.getMessage(), e);
            throw new RuntimeException("Redis 일일 티켓 초기화 실패", e);
        }
    }
}
