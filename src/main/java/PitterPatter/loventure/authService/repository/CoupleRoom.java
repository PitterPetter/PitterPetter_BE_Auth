package PitterPatter.loventure.authService.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "couple_rooms")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoupleRoom {
    
    @Id
    @Column(name = "invite_code", length = 6)
    private String inviteCode;
    
    @Column(name = "couple_id")
    private String coupleId;
    
    @Column(name = "creator_user_id", nullable = false)
    private String creatorUserId;
    
    @Column(name = "partner_user_id")
    private String partnerUserId;
    
    @Column(name = "couple_home_name", length = 100)
    private String coupleHomeName;
    
    @Column(name = "dating_start_date")
    private LocalDate datingStartDate;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private CoupleStatus status = CoupleStatus.PENDING;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    public enum CoupleStatus {
        ACTIVE, DEACTIVED, PENDING
    }
}

