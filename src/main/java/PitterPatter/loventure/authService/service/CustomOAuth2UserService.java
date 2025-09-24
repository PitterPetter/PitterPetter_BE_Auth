package PitterPatter.loventure.authService.service; // 서비스 패키지로 경로를 잡아주세요.

import PitterPatter.loventure.authService.dto.GoogleUserInfo;
import PitterPatter.loventure.authService.dto.KakaoUserInfo;
import PitterPatter.loventure.authService.dto.OAuth2UserInfo;
import PitterPatter.loventure.authService.repository.AccountStatus;
import PitterPatter.loventure.authService.repository.ProviderType;
import PitterPatter.loventure.authService.repository.User;
import PitterPatter.loventure.authService.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    // 카카오, 구글로 부터 전달된 정보 가공 및 DB 연동
    private final UserRepository userRepository;

    @Override
    // OAuth로 부터 온 사용자 정보 수신
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 1. 기본 OAuth2UserService를 통해 사용자 정보를 가져옵니다.
        OAuth2User oAuth2User = super.loadUser(userRequest);
        log.info("oAuth2User attributes: {}", oAuth2User.getAttributes());

        // 2. 로그인 제공자(provider)를 확인합니다. (google, kakao 등)
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        // 3. 제공자에 따라 사용자 정보를 표준화된 DTO로 변환합니다.
        OAuth2UserInfo oAuth2UserInfo = null;
        if (registrationId.equals("kakao")) {
            oAuth2UserInfo = new KakaoUserInfo(oAuth2User.getAttributes());
        } else if (registrationId.equals("google")) {
            oAuth2UserInfo = new GoogleUserInfo(oAuth2User.getAttributes());
        } else {
            // 다른 소셜 로그인 추가 시 로직 확장
            return null;
        }

        String providerId = oAuth2UserInfo.getProviderId();
        String email = oAuth2UserInfo.getEmail();

        // 4. DB에서 사용자를 조회합니다.
        User existUser = userRepository.findByProviderId(providerId);

        if (existUser == null) {
            // 5. 신규 유저인 경우, DB에 저장 (자동 회원가입)
            // OAuth 플랫폼 (카카오, 구글)로 회원가입까지 하려면 비지니스 앱이어야해서 처리 로직을 추가함
            User newUser = User.builder()
                    .providerType(ProviderType.valueOf(registrationId.toUpperCase()))
                    .providerId(providerId)
                    .email(email)
                    .name(oAuth2UserInfo.getName())
                    .status(AccountStatus.ACTIVE) // 기본 상태는 활성
                    .build();
            userRepository.save(newUser);

            return new DefaultOAuth2User(
                    Collections.emptyList(), // 신규 유저는 권한이 없음 (필요시 추가)
                    oAuth2User.getAttributes(),
                    userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName()
            );

        } else {
            // 6. 기존 유저인 경우, 정보를 반환
            // (선택) 정보가 변경되었다면 여기서 업데이트 로직 추가 가능

            return new DefaultOAuth2User(
                    Collections.emptyList(), // 기존 유저의 권한 정보 조회 후 추가
                    oAuth2User.getAttributes(),
                    userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName()
            );
        }
    }
}