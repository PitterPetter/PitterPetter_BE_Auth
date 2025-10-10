package PitterPatter.loventure.authService.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.math.BigInteger;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, BigInteger> {
    User findByProviderId(String providerId);
    User findByEmail(String email);
    Optional<User> findByUserId(BigInteger userId);
}
