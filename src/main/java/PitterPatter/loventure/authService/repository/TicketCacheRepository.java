package PitterPatter.loventure.authService.repository;


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
    private static final int DEFAULT_TTL_SECONDS = 24 * 60 * 60; // 1일

    /**
     * 티켓 정보 저장 (Gateway에서만 사용)
     * Write-Through 패턴에서는 Auth Service가 Redis에 직접 쓰지 않음
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

    // Write-Through 패턴에서는 Auth Service가 Redis에 직접 쓰지 않으므로
    // 삭제, 키 조회, 일일 초기화 등의 메서드는 불필요합니다.
    // Redis 관리는 Gateway에서 담당합니다.
}

