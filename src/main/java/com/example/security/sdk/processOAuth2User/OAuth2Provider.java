package com.example.security.sdk.processOAuth2User;

import java.util.Map;

import com.example.security.dto.Oauth2UserInfoDto;

public interface OAuth2Provider {
    Oauth2UserInfoDto extractUserInfo(Map<String, Object> attributes);
}
