package com.example.security.sdk.processOAuth2User;

import java.util.Map;
import com.example.security.dto.Oauth2UserInfoDto;

public class FacebookOAuth2Provider implements OAuth2Provider {

    @Override
    public Oauth2UserInfoDto extractUserInfo(Map<String, Object> attributes) {
        Map<String, Object> pictureData = (Map<String, Object>) ((Map<String, Object>) attributes.get("picture")).get("data");

        return Oauth2UserInfoDto.builder()
                .id((String) attributes.get("id"))
                .name((String) attributes.get("name"))
                .email((String) attributes.get("email"))
                .picture((String) pictureData.get("url"))
                .build();
    }
}