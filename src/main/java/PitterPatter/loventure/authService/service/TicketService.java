package PitterPatter.loventure.authService.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import PitterPatter.loventure.authService.domain.Couple;
import PitterPatter.loventure.authService.dto.TicketInfo;
import PitterPatter.loventure.authService.exception.CoupleNotFoundException;
import PitterPatter.loventure.authService.repository.CoupleRepository;
import PitterPatter.loventure.authService.repository.TicketCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

    private final CoupleRepository coupleRepository;
    private final TicketCacheRepository ticketCacheRepository;

    /**
     * 티켓 정보 조회 (Redis 우선, 없으면 DB에서 조회 후 Redis에 저장)
     * Gateway 연동용 메서드
     */
    @Transactional(readOnly = true)
    public TicketInfo getTicketInfo(String coupleId) {
        try {
            // 1. Redis에서 먼저 조회
            TicketInfo cachedTicketInfo = ticketCacheRepository.findByCoupleId(coupleId);
            if (cachedTicketInfo != null) {
                log.info("🎫 Redis에서 티켓 정보 조회 성공 - coupleId: {}", coupleId);
                return cachedTicketInfo;
            }
            
            // 2. Redis에 없으면 DB에서 조회
            log.info("🔍 Redis에 데이터 없음, DB에서 조회 - coupleId: {}", coupleId);
            Couple couple = findCoupleById(coupleId);
            
            // 3. DB 데이터를 TicketInfo로 변환하고 Redis에 저장
            TicketInfo ticketInfo = convertToTicketInfo(couple);
            ticketCacheRepository.save(coupleId, ticketInfo);
            
            return ticketInfo;
            
        } catch (Exception e) {
            log.error("🚨 티켓 정보 조회 실패 - coupleId: {}, error: {}", coupleId, e.getMessage(), e);
            throw new RuntimeException("티켓 정보 조회 실패", e);
        }
    }

    // Write-Through 패턴에서는 티켓 추가/삭제 로직이 불필요합니다.
    // Gateway에서 Redis에 직접 관리하고, Auth Service는 동기화만 담당합니다.

    /**
     * Write-Through 패턴용 티켓 정보 업데이트
     * Gateway에서 Redis Stream 이벤트를 통해 자동 호출되는 메서드
     */
    @Transactional
    public void updateTicketInfoFromGateway(String coupleId, Object ticketData) {
        try {
            log.info("🔄 Write-Through 패턴으로 티켓 정보 업데이트 - coupleId: {}", coupleId);
            
            // ticketData를 TicketInfo로 변환
            TicketInfo ticketInfo = convertObjectToTicketInfo(ticketData);
            
            // 1. DB 업데이트
            Couple couple = findCoupleById(coupleId);
            couple.setTicketCount(ticketInfo.ticket());
            couple.setIsTodayTicket(ticketInfo.isTodayTicket());
            couple.setLastSyncedAt(ticketInfo.lastSyncedAt());
            coupleRepository.save(couple);
            
            log.info("✅ Write-Through 티켓 정보 업데이트 완료 - coupleId: {}, ticket: {}", 
                    coupleId, ticketInfo.ticket());
            
        } catch (Exception e) {
            log.error("🚨 Write-Through 티켓 정보 업데이트 실패 - coupleId: {}, error: {}", 
                    coupleId, e.getMessage(), e);
            // Write-Through 패턴에서는 에러를 로그만 남기고 예외를 던지지 않음
            // Gateway의 Redis 데이터가 우선이므로 Auth Service 동기화 실패는 치명적이지 않음
        }
    }

    /**
     * Object를 TicketInfo로 변환하는 헬퍼 메서드
     */
    private TicketInfo convertObjectToTicketInfo(Object ticketData) {
        try {
            if (ticketData instanceof Map<?, ?> map) {
                String coupleId = String.valueOf(map.get("coupleId"));
                Integer ticket = (Integer) map.get("ticket");
                String isTodayTicketStr = String.valueOf(map.get("isTodayTicket"));
                String lastSyncedAtStr = String.valueOf(map.get("lastSyncedAt"));
                
                boolean isTodayTicket = "true".equals(isTodayTicketStr);
                LocalDateTime lastSyncedAt = LocalDateTime.parse(lastSyncedAtStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                
                return new TicketInfo(coupleId, ticket, isTodayTicket, lastSyncedAt);
            }
            throw new IllegalArgumentException("지원하지 않는 ticketData 타입: " + (ticketData != null ? ticketData.getClass() : "null"));
        } catch (Exception e) {
            log.error("🚨 TicketInfo 변환 실패 - ticketData: {}, error: {}", ticketData, e.getMessage());
            throw new RuntimeException("TicketInfo 변환 실패", e);
        }
    }

    /**
     * 일일 티켓 초기화 (매일 정각)
     * Write-Through 패턴에서는 DB만 초기화 (Redis는 Gateway에서 관리)
     */
    @Transactional
    public void resetAllDailyTickets() {
        try {
            LocalDateTime now = LocalDateTime.now();
            
            // DB에서 모든 isTodayTicket을 true로 변경
            int updatedCount = coupleRepository.resetAllDailyTickets(now);
            
            log.info("🎉 일일 티켓 초기화 완료 - DB 업데이트: {}건", updatedCount);
            
        } catch (Exception e) {
            log.error("🚨 일일 티켓 초기화 실패 - error: {}", e.getMessage(), e);
            throw new RuntimeException("일일 티켓 초기화 실패", e);
        }
    }

    // Write-Through 패턴에서는 조회 기능만 필요합니다.
    // 티켓 추가/삭제는 Gateway에서 Redis를 통해 처리됩니다.

    /**
     * Couple 조회 헬퍼 메서드
     */
    private Couple findCoupleById(String coupleId) {
        return coupleRepository.findById(coupleId)
            .orElseThrow(() -> new CoupleNotFoundException("커플 정보를 찾을 수 없습니다."));
    }


    /**
     * Couple 엔티티를 TicketInfo로 변환
     */
    private TicketInfo convertToTicketInfo(Couple couple) {
        return new TicketInfo(
            couple.getCoupleId(),
            couple.getTicketCount(),
            couple.getIsTodayTicket(),
            couple.getLastSyncedAt()
        );
    }

}