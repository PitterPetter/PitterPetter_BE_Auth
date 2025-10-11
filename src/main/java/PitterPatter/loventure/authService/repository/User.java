package PitterPatter.loventure.authService.repository;

import java.math.BigInteger;
import java.time.LocalDate;
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
import lombok.Setter; // Setter 추가

@Entity
@Getter
@Setter // RefreshToken 저장을 위해 @Setter 추가
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

    @Column(name = "refresh_token", length = 500)
    private String refreshToken;

    @Column(length = 100)
    private String name;

    @Column(length = 50)
    private String nickname;

    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private Integer alcoholPreference; //1~5

    private Integer activeBound; //1~5

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
            // TSID를 BigInteger로 변환 (TSID는 64비트 정수)
            this.userId = BigInteger.valueOf(TsidCreator.getTsid().toLong());
        }
    }
    
    // 사용자 정보 업데이트 메서드
    public void updateUserInfo(String email, String name) {
        this.email = email;
        this.name = name;
    }
    
    // 닉네임 업데이트 메서드
    public void setNickname(String nickname) {
        this.nickname = nickname;
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
    
    // 프로필 수정을 위한 setter 메서드들
    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }
    
    public void setGender(Gender gender) {
        this.gender = gender;
    }
    
    public void setAlcoholPreference(Integer alcoholPreference) {
        this.alcoholPreference = alcoholPreference;
    }
    
    public void setActiveBound(Integer activeBound) {
        this.activeBound = activeBound;
    }
    
    public void setFavoriteFoodCategories(List<FavoriteFoodCategories> favoriteFoodCategories) {
        this.favoriteFoodCategories = favoriteFoodCategories;
    }
    
    public void setDateCostPreference(DateCostPreference dateCostPreference) {
        this.dateCostPreference = dateCostPreference;
    }
    
    public void setPreferredAtmosphere(String preferredAtmosphere) {
        this.preferredAtmosphere = preferredAtmosphere;
    }
}
