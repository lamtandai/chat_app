package com.example.security.sdk.processOAuth2User;

import java.util.Map;
import com.example.security.dto.Oauth2UserInfoDto;

public class GoogleOAuth2Provider implements OAuth2Provider {

    @Override
    public Oauth2UserInfoDto extractUserInfo(Map<String, Object> attributes) {
        return Oauth2UserInfoDto.builder()
                .id((String) attributes.get("sub"))
                .name((String) attributes.get("name"))
                .email((String) attributes.get("email"))
                .picture((String) attributes.get("picture"))
                .build();
    }
}
