package PitterPatter.loventure.authService.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import PitterPatter.loventure.authService.domain.Couple;

@Repository
public interface CoupleRepository extends JpaRepository<Couple, String> {
    
    /**
     * 커플 ID로 커플 정보 조회 (PK이므로 기본 메서드 사용)
     */
    Optional<Couple> findById(String coupleId);
    
    /**
     * 일일 티켓 초기화 (매일 정각에 호출)
     */
    @Modifying
    @Query("UPDATE Couple c SET c.isTodayTicket = true, c.lastSyncedAt = :now WHERE c.isTodayTicket = false")
    int resetAllDailyTickets(@Param("now") LocalDateTime now);
    
    /**
     * 오래된 동기화 데이터 조회 (정리용)
     */
    @Query("SELECT c FROM Couple c WHERE c.lastSyncedAt < :cutoffTime")
    List<Couple> findStaleTickets(@Param("cutoffTime") LocalDateTime cutoffTime);
}
