package PitterPatter.loventure.authService.repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.github.f4b6a3.tsid.TsidCreator;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

enum Gender {
    MALE, FEMALE, OTHER
}

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProviderType providerType;

    @Column(nullable = false, unique = true)
    private String providerId;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(length = 20)
    private String name;

    private LocalDateTime birthDate;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private Integer alcoholPreference; //0~100 사이의 값으로 설정해서 슬라이더로 시각화 예정

    private Integer activeBound; //0~100 사이의 값으로 설정해서 슬라이더로 시각화 예정

    @ElementCollection(fetch = FetchType.LAZY)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "userFavoriteFoods", joinColumns = @JoinColumn(name = "userId"))
    @Column(name = "favoriteFoodCategories")
    @Builder.Default
    private List<FavoriteFoodCategories> favoriteFoodCategories = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private DateCostPreference dateCostPreference;

    private String preferredAtmosphere;

    @Column(unique = true, nullable = false)
    private Long tsid;

    @Enumerated(EnumType.STRING)
    private AccountStatus status;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private Integer rerollCount;

    @PrePersist
    public void createTsid() {
        if (this.tsid == null) {
            this.tsid = TsidCreator.getTsid().toLong();
        }
    }
    
    // 사용자 정보 업데이트 메서드
    public void updateUserInfo(String email, String name) {
        this.email = email;
        this.name = name;
    }
    
    // 사용자 상태 변경 메서드
    public void setStatus(AccountStatus status) {
        this.status = status;
    }
}
