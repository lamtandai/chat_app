package com.example.security.sdk.processOAuth2User;

import java.util.Map;

import com.example.security.dto.Oauth2UserInfoDto;

public class GithubOAuth2Provider implements OAuth2Provider {

    @Override
    public Oauth2UserInfoDto extractUserInfo(Map<String, Object> attributes) {
        // GitHub might not provide email if user's email is private
        String email = (String) attributes.get("email");
        String login = (String) attributes.get("login");
        String name = (String) attributes.get("name");
        Object idObj = attributes.get("id");

        // Ensure ID is never null
        String id = idObj != null ? String.valueOf(idObj) : "unknown";

        // Ensure login is never null (GitHub should always provide this)
        if (login == null || login.trim().isEmpty()) {
            login = "github-user-" + id;
        }

        // Use login as fallback if email is not available
        String username = (email != null && !email.trim().isEmpty()) ? email : (login + "@github.local");

        return Oauth2UserInfoDto.builder()
                .id(id)
                .name((name != null && !name.trim().isEmpty()) ? name : login) // Use login as fallback for name
                .email(username)
                .picture((String) attributes.get("avatar_url"))
                .build();
    }
}