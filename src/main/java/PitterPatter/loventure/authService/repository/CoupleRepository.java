package PitterPatter.loventure.authService.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import PitterPatter.loventure.authService.domain.Couple;

/**
 * 커플 정보 Repository
 */
@Repository
public interface CoupleRepository extends JpaRepository<Couple, String> {
    
    /**
     * 일일 티켓 초기화 (모든 isTodayTicket을 true로 변경)
     */
    @Modifying
    @Query("UPDATE Couple c SET c.isTodayTicket = true, c.lastSyncedAt = :now")
    int resetAllDailyTickets(@Param("now") LocalDateTime now);
}
