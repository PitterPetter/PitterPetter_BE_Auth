package PitterPatter.loventure.authService.domain;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "couple")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Couple {

    @Id
    @Column(name = "couple_id", length = 50)
    private String coupleId;

    // 일반 티켓 잔액
    @Column(name = "ticket_count", nullable = false)
    @Builder.Default
    private Integer ticketCount = 2;

    // 오늘 티켓을 사용했는지 여부
    @Column(name = "is_today_ticket", nullable = false)
    @Builder.Default
    private Boolean isTodayTicket = true;

    // 마지막 동기화 시간
    @Column(name = "last_synced_at", nullable = false)
    private LocalDateTime lastSyncedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 비즈니스 로직: 오늘 티켓 추가 (isTodayTicket이 true일 때만)
    public boolean addTodayTicket() {
        if (!this.isTodayTicket) {
            return false; // 이미 오늘 티켓을 사용했음
        }
        
        this.ticketCount++;
        this.isTodayTicket = false;
        this.lastSyncedAt = LocalDateTime.now();
        return true;
    }

    // 비즈니스 로직: 티켓 사용 (ticketCount가 0보다 클 때만)
    public boolean useTicket() {
        if (this.ticketCount <= 0) {
            return false;
        }
        
        this.ticketCount--;
        this.lastSyncedAt = LocalDateTime.now();
        return true;
    }

    // 티켓 잔액 확인
    public boolean hasAvailableTicket() {
        return this.ticketCount > 0;
    }

    // 오늘 티켓 사용 가능 여부 확인
    public boolean canUseTodayTicket() {
        return this.isTodayTicket;
    }

    // 일일 티켓 초기화 (매일 정각에 호출)
    public void resetDailyTicket() {
        this.isTodayTicket = true;
        this.lastSyncedAt = LocalDateTime.now();
    }
}
