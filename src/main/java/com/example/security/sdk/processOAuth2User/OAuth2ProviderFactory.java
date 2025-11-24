package com.example.security.sdk.processOAuth2User;

import java.util.HashMap;
import java.util.Map;

public class OAuth2ProviderFactory {

    private static final Map<String, OAuth2Provider> providers = new HashMap<>();

    static {
        providers.put("google", new GoogleOAuth2Provider());
        providers.put("github", new GithubOAuth2Provider());
        providers.put("facebook", new FacebookOAuth2Provider());
    }

    public static OAuth2Provider getProvider(String registrationId) {
        return providers.getOrDefault(registrationId, attributes -> {
            throw new IllegalArgumentException("Unsupported provider: " + registrationId);
        });
    }
}