package PitterPatter.loventure.authService.service;

import java.math.BigInteger;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import PitterPatter.loventure.authService.dto.UserDto;
import PitterPatter.loventure.authService.dto.request.OnboardingRequest;
import PitterPatter.loventure.authService.exception.BusinessException;
import PitterPatter.loventure.authService.exception.ErrorCode;
import PitterPatter.loventure.authService.repository.User;
import PitterPatter.loventure.authService.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public UserDto updateOnboardingInfo(String providerId, OnboardingRequest request) {
        User user = userRepository.findByProviderId(providerId);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다: " + providerId);
        }


        // 닉네임, 생년월일, 성별 업데이트
        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getBirthDate() != null) {
            user.setBirthDate(request.getBirthDate());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }

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
     * userId로 사용자 검증 및 조회 (BigInteger userId 사용)
     */
    public User validateUserByUserId(String userId) {
        return userRepository.findByUserId(new BigInteger(userId))
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
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
     * userId로 사용자 조회 (BigInteger)
     */
    public User getUserById(BigInteger userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
    
    /**
     * userId로 사용자 조회 (String)
     */
    public User getUserById(String userId) {
        try {
            BigInteger userIdBigInt = new BigInteger(userId);
            return getUserById(userIdBigInt);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.INVALID_USER_ID_FORMAT);
        }
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