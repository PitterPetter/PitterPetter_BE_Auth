package PitterPatter.loventure.authService.repository;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByProviderId(String providerId);
    User findByEmail(String email);
    User findByTsid(Long tsid);
}
