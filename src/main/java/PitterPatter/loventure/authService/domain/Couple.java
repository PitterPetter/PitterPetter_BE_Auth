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

    // 일반 티켓 잔액은 CoupleRoom 테이블에서 관리

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

    // Write-Through 패턴에서는 비즈니스 로직이 Gateway에서 처리되므로
    // Couple 엔티티는 단순한 데이터 저장소 역할만 합니다.

    // 일일 티켓 초기화 (매일 정각에 호출)
    public void resetDailyTicket() {
        this.isTodayTicket = true;
        this.lastSyncedAt = LocalDateTime.now();
    }
}
