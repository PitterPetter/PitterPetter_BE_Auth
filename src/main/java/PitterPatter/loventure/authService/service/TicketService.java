package PitterPatter.loventure.authService.service;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import PitterPatter.loventure.authService.domain.Couple;
import PitterPatter.loventure.authService.dto.TicketBalanceResponse;
import PitterPatter.loventure.authService.dto.TicketInfo;
import PitterPatter.loventure.authService.exception.CoupleNotFoundException;
import PitterPatter.loventure.authService.exception.InsufficientTicketException;
import PitterPatter.loventure.authService.repository.CoupleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

    private final CoupleRepository coupleRepository;
    private final RedisService redisService;
    private final ObjectMapper objectMapper;

    /**
     * 티켓 정보 조회 (Redis 우선, 없으면 DB에서 조회 후 Redis에 저장)
     * Gateway 연동용 메서드
     */
    @Transactional(readOnly = true)
    public TicketInfo getTicketInfo(String coupleId) {
        try {
            // 1. Redis에서 먼저 조회
            Object redisData = redisService.getCoupleTicketInfo(coupleId);
            if (redisData != null) {
                log.info("🎫 Redis에서 티켓 정보 조회 성공 - coupleId: {}", coupleId);
                return convertToTicketInfo(redisData);
            }
            
            // 2. Redis에 없으면 DB에서 조회
            log.info("🔍 Redis에 데이터 없음, DB에서 조회 - coupleId: {}", coupleId);
            Couple couple = findCoupleById(coupleId);
            
            // 3. DB 데이터를 Redis에 저장
            TicketInfo ticketInfo = convertToTicketInfo(couple);
            redisService.updateCoupleTicketInfo(coupleId, ticketInfo);
            
            return ticketInfo;
            
        } catch (Exception e) {
            log.error("🚨 티켓 정보 조회 실패 - coupleId: {}, error: {}", coupleId, e.getMessage(), e);
            throw new RuntimeException("티켓 정보 조회 실패", e);
        }
    }

    /**
     * 티켓 추가 (코스 저장 시)
     * isTodayTicket이 true일 때만 티켓 추가
     */
    @Transactional
    public boolean addTodayTicket(String coupleId) {
        try {
            // 1. 현재 티켓 정보 조회
            TicketInfo currentTicket = getTicketInfo(coupleId);
            
            // 2. isTodayTicket이 true인지 확인
            if (!currentTicket.getIsTodayTicket()) {
                log.warn("❌ 이미 오늘 티켓을 사용함 - coupleId: {}", coupleId);
                return false;
            }
            
            // 3. DB에서 티켓 추가
            Couple couple = findCoupleById(coupleId);
            boolean added = couple.addTodayTicket();
            
            if (added) {
                coupleRepository.save(couple);
                
                // 4. Redis 업데이트
                TicketInfo updatedTicket = convertToTicketInfo(couple);
                redisService.updateCoupleTicketInfo(coupleId, updatedTicket);
                
                log.info("✅ 티켓 추가 완료 - coupleId: {}, ticket: {} → {}", 
                        coupleId, currentTicket.getTicket(), updatedTicket.getTicket());
            }
            
            return added;
            
        } catch (Exception e) {
            log.error("🚨 티켓 추가 실패 - coupleId: {}, error: {}", coupleId, e.getMessage(), e);
            throw new RuntimeException("티켓 추가 실패", e);
        }
    }

    /**
     * 티켓 사용 (지역락 해제 시)
     * ticketCount가 0보다 클 때만 티켓 사용
     */
    @Transactional
    public void useTicket(String coupleId) {
        try {
            // 1. 현재 티켓 정보 조회
            TicketInfo currentTicket = getTicketInfo(coupleId);
            
            // 2. 티켓이 있는지 확인
            if (currentTicket.getTicket() <= 0) {
                throw new InsufficientTicketException("티켓이 부족합니다.");
            }
            
            // 3. DB에서 티켓 사용
            Couple couple = findCoupleById(coupleId);
            boolean used = couple.useTicket();
            
            if (!used) {
                throw new InsufficientTicketException("티켓이 부족합니다.");
            }
            
            coupleRepository.save(couple);
            
            // 4. Redis 업데이트
            TicketInfo updatedTicket = convertToTicketInfo(couple);
            redisService.updateCoupleTicketInfo(coupleId, updatedTicket);
            
            log.info("✅ 티켓 사용 완료 - coupleId: {}, ticket: {} → {}", 
                    coupleId, currentTicket.getTicket(), updatedTicket.getTicket());
            
        } catch (InsufficientTicketException e) {
            throw e;
        } catch (Exception e) {
            log.error("🚨 티켓 사용 실패 - coupleId: {}, error: {}", coupleId, e.getMessage(), e);
            throw new RuntimeException("티켓 사용 실패", e);
        }
    }

    /**
     * 티켓 정보 업데이트 (Gateway에서 호출)
     */
    @Transactional
    public void updateTicketInfo(String coupleId, TicketInfo ticketInfo) {
        try {
            // 1. DB 업데이트
            Couple couple = findCoupleById(coupleId);
            couple.setTicketCount(ticketInfo.getTicket());
            couple.setIsTodayTicket(ticketInfo.getIsTodayTicket());
            couple.setLastSyncedAt(ticketInfo.getLastSyncedAt());
            coupleRepository.save(couple);
            
            // 2. Redis 업데이트
            redisService.updateCoupleTicketInfo(coupleId, ticketInfo);
            
            log.info("✅ 티켓 정보 업데이트 완료 - coupleId: {}, ticket: {}", coupleId, ticketInfo.getTicket());
            
        } catch (Exception e) {
            log.error("🚨 티켓 정보 업데이트 실패 - coupleId: {}, error: {}", coupleId, e.getMessage(), e);
            throw new RuntimeException("티켓 정보 업데이트 실패", e);
        }
    }

    /**
     * 일일 티켓 초기화 (매일 정각)
     */
    @Transactional
    public void resetAllDailyTickets() {
        try {
            LocalDateTime now = LocalDateTime.now();
            
            // 1. DB에서 모든 isTodayTicket을 true로 변경
            int updatedCount = coupleRepository.resetAllDailyTickets(now);
            
            // 2. Redis에서 모든 coupleId 키 조회하여 업데이트
            resetRedisDailyTickets();
            
            log.info("🎉 일일 티켓 초기화 완료 - DB 업데이트: {}건", updatedCount);
            
        } catch (Exception e) {
            log.error("🚨 일일 티켓 초기화 실패 - error: {}", e.getMessage(), e);
            throw new RuntimeException("일일 티켓 초기화 실패", e);
        }
    }

    /**
     * 티켓 잔액 조회 (기존 API 호환성)
     */
    @Transactional(readOnly = true)
    public int getDailyTicketBalance(String coupleId) {
        TicketInfo ticketInfo = getTicketInfo(coupleId);
        return ticketInfo.getTicket();
    }

    /**
     * 티켓 전체 정보 조회 (기존 API 호환성)
     */
    @Transactional(readOnly = true)
    public TicketBalanceResponse getTicketBalanceResponse(String coupleId) {
        TicketInfo ticketInfo = getTicketInfo(coupleId);
        
        return TicketBalanceResponse.builder()
            .coupleId(coupleId)
            .ticket(ticketInfo.getTicket())
            .isTodayTicket(ticketInfo.getIsTodayTicket())
            .lastSyncedAt(ticketInfo.getLastSyncedAt())
            .build();
    }

    /**
     * Couple 조회 헬퍼 메서드
     */
    private Couple findCoupleById(String coupleId) {
        return coupleRepository.findById(coupleId)
            .orElseThrow(() -> new CoupleNotFoundException("커플 정보를 찾을 수 없습니다."));
    }

    /**
     * Redis 데이터를 TicketInfo로 변환
     */
    private TicketInfo convertToTicketInfo(Object redisData) {
        try {
            if (redisData instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) redisData;
                return TicketInfo.builder()
                    .coupleId(String.valueOf(map.get("coupleId")))
                    .ticket((Integer) map.get("ticket"))
                    .isTodayTicket(Boolean.valueOf(String.valueOf(map.get("isTodayTicket"))))
                    .lastSyncedAt(LocalDateTime.parse(String.valueOf(map.get("lastSyncedAt"))))
                    .build();
            } else {
                return objectMapper.convertValue(redisData, TicketInfo.class);
            }
        } catch (Exception e) {
            log.error("🚨 Redis 데이터 변환 실패 - data: {}, error: {}", redisData, e.getMessage());
            throw new RuntimeException("데이터 변환 실패", e);
        }
    }

    /**
     * Couple 엔티티를 TicketInfo로 변환
     */
    private TicketInfo convertToTicketInfo(Couple couple) {
        return TicketInfo.builder()
            .coupleId(couple.getCoupleId())
            .ticket(couple.getTicketCount())
            .isTodayTicket(couple.getIsTodayTicket())
            .lastSyncedAt(couple.getLastSyncedAt())
            .build();
    }

    /**
     * Redis에서 일일 티켓 초기화
     */
    private void resetRedisDailyTickets() {
        // Redis에서 모든 coupleId:* 패턴의 키 조회
        var keys = redisService.getKeys("coupleId:*");
        
        for (String key : keys) {
            try {
                Object data = redisService.getValue(key);
                if (data != null) {
                    TicketInfo ticketInfo = convertToTicketInfo(data);
                    ticketInfo = TicketInfo.builder()
                        .coupleId(ticketInfo.getCoupleId())
                        .ticket(ticketInfo.getTicket())
                        .isTodayTicket(true)
                        .lastSyncedAt(LocalDateTime.now())
                        .build();
                    
                    redisService.setValue(key, ticketInfo);
                }
            } catch (Exception e) {
                log.warn("⚠️ Redis 키 초기화 실패 - key: {}, error: {}", key, e.getMessage());
            }
        }
    }
}