package PitterPatter.loventure.authService.dto;

import java.util.Map;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class KakaoUserInfo implements OAuth2UserInfo{
    private final Map<String, Object> attribute;

    @Override
    public String getProvider() {
        return "kakao";
    }

    @Override
    public String getProviderId() {
        return attribute.get("id").toString();
    }

    @Override
    @SuppressWarnings("unchecked")
    public String getName() {
        return ((Map<String, Object>) attribute.get("properties")).get("nickname").toString();
    }

    @Override
    @SuppressWarnings("unchecked")
    public String getEmail() {
        return ((Map<String, Object>) attribute.get("kakao_account")).get("email").toString();
    }
}
