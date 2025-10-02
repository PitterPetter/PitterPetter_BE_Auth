package PitterPatter.loventure.authService.service;

import java.math.BigInteger;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import PitterPatter.loventure.authService.repository.User;
import PitterPatter.loventure.authService.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 사용자 검증 관련 공통 로직을 담당하는 서비스
 * 중복된 사용자 검증 로직을 통합하여 관리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserValidationService {

    private final UserRepository userRepository;

    /**
     * ProviderId로 사용자 검증 및 조회
     */
    public User validateUserByProviderId(String providerId) {
        if (providerId == null || providerId.trim().isEmpty()) {
            throw new IllegalArgumentException("ProviderId는 필수입니다");
        }
        
        User user = userRepository.findByProviderId(providerId);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다: " + providerId);
        }
        return user;
    }

    /**
     * userId로 사용자 검증 및 조회 (BigInteger userId 사용)
     */
    public User validateUserByUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("UserId는 필수입니다");
        }
        
        try {
            BigInteger userIdBigInt = new BigInteger(userId);
            return userRepository.findByUserId(userIdBigInt)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("잘못된 사용자 ID 형식입니다: " + userId);
        }
    }

    /**
     * UserDetails에서 providerId 추출
     */
    public String extractProviderId(UserDetails userDetails) {
        if (userDetails == null) {
            throw new IllegalArgumentException("인증된 사용자 정보를 찾을 수 없습니다");
        }
        return userDetails.getUsername();
    }

    /**
     * UserDetails에서 User 엔티티 조회
     */
    public User getUserFromUserDetails(UserDetails userDetails) {
        String providerId = extractProviderId(userDetails);
        return validateUserByProviderId(providerId);
    }

    /**
     * UserDetails에서 userId 추출
     */
    public String extractUserId(UserDetails userDetails) {
        User user = getUserFromUserDetails(userDetails);
        return user.getUserId().toString();
    }

    /**
     * 사용자 계정 상태 검증
     */
    public void validateUserStatus(User user) {
        if (user.getStatus() == null) {
            throw new IllegalStateException("사용자 계정 상태가 설정되지 않았습니다");
        }
        
        if (!user.getStatus().name().equals("ACTIVE")) {
            throw new IllegalStateException("비활성화된 계정입니다");
        }
    }
}
