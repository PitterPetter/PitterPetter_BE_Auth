package PitterPatter.loventure.authService.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CoupleOnboardingRepository extends JpaRepository<CoupleOnboarding, String> {

    Optional<CoupleOnboarding> findByCoupleId(String coupleId);
}

