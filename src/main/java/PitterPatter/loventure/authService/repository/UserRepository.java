package PitterPatter.loventure.authService.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    User findByProviderId(String providerId);
    User findByEmail(String email);
    Optional<User> findByUserId(String userId);
}
