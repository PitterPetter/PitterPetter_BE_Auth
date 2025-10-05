package PitterPatter.loventure.authService.repository;

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
@Table(name = "couple_onboardings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoupleOnboarding {
    
    @Id
    @Column(name = "onboarding_id")
    private String onboardingId;
    
    @Column(name = "couple_id", nullable = false)
    private String coupleId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "today_condition")
    private TodayCondition todayCondition;
    
    @Column(name = "drinking", length = 1)
    private String drinking;
    
    @Column(name = "hate_food")
    private String hateFood;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

