package PitterPatter.loventure.authService.service;

import java.util.Set;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Redis 연동 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Redis에서 값 조회
     */
    public Object getValue(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Redis 조회 실패 - key: {}, error: {}", key, e.getMessage());
            return null;
        }
    }

    /**
     * Redis에 값 저장
     */
    public void setValue(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            log.info("Redis 저장 성공 - key: {}, value: {}", key, value);
        } catch (Exception e) {
            log.error("Redis 저장 실패 - key: {}, value: {}, error: {}", key, value, e.getMessage());
        }
    }

    /**
     * Redis에서 값 삭제
     */
    public void deleteValue(String key) {
        try {
            redisTemplate.delete(key);
            log.info("Redis 삭제 성공 - key: {}", key);
        } catch (Exception e) {
            log.error("Redis 삭제 실패 - key: {}, error: {}", key, e.getMessage());
        }
    }

    /**
     * Redis에서 패턴으로 키 조회
     */
    public Set<String> getKeys(String pattern) {
        try {
            return redisTemplate.keys(pattern);
        } catch (Exception e) {
            log.error("Redis 키 조회 실패 - pattern: {}, error: {}", pattern, e.getMessage());
            return Set.of();
        }
    }

    /**
     * Redis 전체 데이터 삭제
     */
    public void flushAll() {
        try {
            redisTemplate.getConnectionFactory().getConnection().flushAll();
            log.info("Redis 전체 데이터 삭제 완료");
        } catch (Exception e) {
            log.error("Redis 전체 데이터 삭제 실패 - error: {}", e.getMessage());
        }
    }

    /**
     * Redis 현재 데이터베이스 삭제
     */
    public void flushDb() {
        try {
            redisTemplate.getConnectionFactory().getConnection().flushDb();
            log.info("Redis 현재 데이터베이스 삭제 완료");
        } catch (Exception e) {
            log.error("Redis 현재 데이터베이스 삭제 실패 - error: {}", e.getMessage());
        }
    }

    /**
     * coupleId로 티켓 정보 조회 (동기식) - Gateway 연동용
     * Key 형식: coupleId:{coupleId}
     */
    public Object getCoupleTicketInfo(String coupleId) {
        String key = "coupleId:" + coupleId;
        Object value = getValue(key);
        log.info("🎫 Gateway 연동 - Redis 조회 - Key: {}, Value: {}", key, value);
        return value;
    }

    /**
     * coupleId로 티켓 정보 업데이트 (동기식) - Gateway 연동용
     * Key 형식: coupleId:{coupleId}
     */
    public void updateCoupleTicketInfo(String coupleId, Object ticketData) {
        String key = "coupleId:" + coupleId;
        setValue(key, ticketData);
        log.info("🎫 Gateway 연동 - Redis 업데이트 - Key: {}, Value: {}", key, ticketData);
    }
}

