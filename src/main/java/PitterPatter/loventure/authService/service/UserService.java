package PitterPatter.loventure.authService.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import PitterPatter.loventure.authService.dto.UserDto;
import PitterPatter.loventure.authService.dto.request.OnboardingRequest;
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
     * TSID로 사용자 검증 및 조회
     */
    public User validateUserByTsid(String tsid) {
        Long tsidLong = Long.parseLong(tsid);
        User user = userRepository.findByTsid(tsidLong);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다: " + tsid);
        }
        return user;
    }
}