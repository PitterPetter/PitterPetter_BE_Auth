package PitterPatter.loventure.authService.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import PitterPatter.loventure.authService.dto.UserDto;
import PitterPatter.loventure.authService.dto.request.OnboardingRequest;
import PitterPatter.loventure.authService.exception.BusinessException;
import PitterPatter.loventure.authService.exception.ErrorCode;
import PitterPatter.loventure.authService.repository.User;
import PitterPatter.loventure.authService.repository.UserRepository;
import PitterPatter.loventure.authService.security.JWTUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final JWTUtil jwtUtil;

    @Transactional
    public UserDto updateOnboardingInfo(String providerId, OnboardingRequest request) {
        User user = userRepository.findByProviderId(providerId);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다: " + providerId);
        }

        // 온보딩에서는 닉네임, 생년월일, 성별을 받지 않음
        // 이 값들은 provider로부터 제공받지 못하면 기본값 null로 두고 mypage에서 수정
        // 따라서 온보딩에서는 선호도 정보만 업데이트

        user.updateOnboardingInfo(
                request.getAlcoholPreference(),
                request.getActiveBound(),
                request.getFavoriteFoodCategories(),
                request.getDateCostPreference(),
                request.getPreferredAtmosphere()
        );

        // 업데이트된 사용자 정보를 DTO로 변환하여 반환
        return UserDto.from(user);
    }

    /**
     * ProviderId로 사용자 검증 및 조회
     */
    public User validateUserByProviderId(String providerId) {
        User user = userRepository.findByProviderId(providerId);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다: " + providerId);
        }
        return user;
    }

    /**
     * 사용자의 온보딩 완료 여부 확인
     */
    public boolean isOnboardingCompleted(User user) {
        return user.getAlcoholPreference() != null &&
               user.getActiveBound() != null &&
               user.getFavoriteFoodCategories() != null &&
               !user.getFavoriteFoodCategories().isEmpty() &&
               user.getDateCostPreference() != null &&
               user.getPreferredAtmosphere() != null;
    }

    /**
     * 사용자의 rock 완료 여부 확인
     * TODO: 실제 rock 관련 필드가 추가되면 해당 필드를 확인하도록 수정 필요
     */
    public boolean isRockCompleted(User user) {
        // 임시로 항상 false 반환 (rock 기능이 아직 구현되지 않음)
        // 실제 구현에서는 rock 관련 필드를 확인해야 함
        return false;
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
     * HttpServletRequest에서 JWT 토큰 추출
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization 헤더를 찾을 수 없습니다");
        }
        
        String token = authorization.split(" ")[1];
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("JWT 토큰이 비어있습니다");
        }
        
        return token;
    }
    
    /**
     * HttpServletRequest에서 coupleId 추출 (JWT에서)
     */
    public String extractCoupleIdFromRequest(HttpServletRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("HttpServletRequest가 null입니다");
        }
        
        // HttpServletRequest에서 JWT 토큰 추출
        String token = extractTokenFromRequest(request);
        
        // JWT에서 coupleId 추출
        String coupleId = jwtUtil.getCoupleIdFromToken(token);
        if (coupleId == null) {
            throw new IllegalArgumentException("JWT에서 coupleId를 찾을 수 없습니다");
        }
        
        return coupleId;
    }
    
    /**
     * userId로 사용자 조회 (String)
     */
    public User getUserById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
    
    
    /**
     * 사용자 삭제 (소프트 삭제)
     */
    @Transactional
    public void deleteUser(User user) {
        user.setStatus(PitterPatter.loventure.authService.repository.AccountStatus.DEACTIVATED);
        userRepository.save(user);
    }
    
    /**
     * 사용자 프로필 수정
     */
    @Transactional
    public User updateProfile(String providerId, PitterPatter.loventure.authService.dto.request.ProfileUpdateRequest request) {
        User user = validateUserByProviderId(providerId);
        
        // null이 아닌 값만 업데이트
        if (request.name() != null) {
            user.updateUserInfo(user.getEmail(), request.name());
        }
        if (request.nickname() != null) {
            user.setNickname(request.nickname());
        }
        if (request.birthDate() != null) {
            user.setBirthDate(request.birthDate());
        }
        if (request.gender() != null) {
            user.setGender(request.gender());
        }
        if (request.alcoholPreference() != null) {
            user.setAlcoholPreference(request.alcoholPreference());
        }
        if (request.activeBound() != null) {
            user.setActiveBound(request.activeBound());
        }
        if (request.favoriteFoodCategories() != null) {
            user.setFavoriteFoodCategories(request.favoriteFoodCategories());
        }
        if (request.dateCostPreference() != null) {
            user.setDateCostPreference(request.dateCostPreference());
        }
        if (request.preferredAtmosphere() != null) {
            user.setPreferredAtmosphere(request.preferredAtmosphere());
        }
        
        return userRepository.save(user);
    }
    
    /**
     * 이메일로 사용자 조회
     */
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    /**
     * ProviderId로 사용자 조회 (null 허용)
     */
    public User getUserByProviderId(String providerId) {
        return userRepository.findByProviderId(providerId);
    }
    
    /**
     * 신규 사용자 생성
     */
    @Transactional
    public User createUser(PitterPatter.loventure.authService.dto.request.SignupRequest signupRequest) {
        User newUser = User.builder()
                .providerType(signupRequest.getProviderType())
                .providerId(signupRequest.getProviderId())
                .email(signupRequest.getEmail())
                .name(signupRequest.getName())
                .status(PitterPatter.loventure.authService.repository.AccountStatus.ACTIVE)
                .build();
        
        return userRepository.save(newUser);
    }
}