package PitterPatter.loventure.authService.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import PitterPatter.loventure.authService.domain.Couple;
import PitterPatter.loventure.authService.dto.TicketBalanceResponse;
import PitterPatter.loventure.authService.dto.TicketInfo;
import PitterPatter.loventure.authService.exception.CoupleNotFoundException;
import PitterPatter.loventure.authService.exception.InsufficientTicketException;
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
            if (!currentTicket.isTodayTicket()) {
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
                ticketCacheRepository.save(coupleId, updatedTicket);
                
                log.info("✅ 티켓 추가 완료 - coupleId: {}, ticket: {} → {}", 
                        coupleId, currentTicket.ticket(), updatedTicket.ticket());
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
            if (currentTicket.ticket() <= 0) {
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
            ticketCacheRepository.save(coupleId, updatedTicket);
            
            log.info("✅ 티켓 사용 완료 - coupleId: {}, ticket: {} → {}", 
                    coupleId, currentTicket.ticket(), updatedTicket.ticket());
            
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
            couple.setTicketCount(ticketInfo.ticket());
            couple.setIsTodayTicket(ticketInfo.isTodayTicket());
            couple.setLastSyncedAt(ticketInfo.lastSyncedAt());
            coupleRepository.save(couple);
            
            // 2. Redis 업데이트
            ticketCacheRepository.save(coupleId, ticketInfo);
            
            log.info("✅ 티켓 정보 업데이트 완료 - coupleId: {}, ticket: {}", coupleId, ticketInfo.ticket());
            
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
            
            // 2. Redis에서 모든 티켓 정보 초기화
            ticketCacheRepository.resetAllDailyTickets();
            
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
        return ticketInfo.ticket();
    }

    /**
     * 티켓 전체 정보 조회 (기존 API 호환성)
     */
    @Transactional(readOnly = true)
    public TicketBalanceResponse getTicketBalanceResponse(String coupleId) {
        TicketInfo ticketInfo = getTicketInfo(coupleId);
        
        return new TicketBalanceResponse(
            coupleId,
            ticketInfo.ticket(),
            ticketInfo.isTodayTicket(),
            ticketInfo.lastSyncedAt()
        );
    }

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