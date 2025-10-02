package PitterPatter.loventure.authService.repository;

import java.math.BigInteger;
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
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "users")
public class User {

    @Id
    @Column(unique = true, nullable = false)
    private BigInteger userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProviderType providerType;

    @Column(nullable = false, unique = true)
    private String providerId;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(length = 100)
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

    @Enumerated(EnumType.STRING)
    private AccountStatus status;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private Integer rerollCount;

    @PrePersist
    public void createUserId() {
        if (this.userId == null) {
            this.userId = new BigInteger(TsidCreator.getTsid().toString());
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

    public void updateOnboardingInfo(Integer alcoholPreference,
                                     Integer activeBound,
                                     List<FavoriteFoodCategories> favoriteFoodCategories,
                                     DateCostPreference dateCostPreference,
                                     String preferredAtmosphere) {
        this.alcoholPreference = alcoholPreference;
        this.activeBound = activeBound;
        this.favoriteFoodCategories = favoriteFoodCategories;
        this.dateCostPreference = dateCostPreference;
        this.preferredAtmosphere = preferredAtmosphere;
    }
}
