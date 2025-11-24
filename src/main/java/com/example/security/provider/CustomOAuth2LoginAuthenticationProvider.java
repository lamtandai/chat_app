package com.example.security.provider;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationProvider;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.user.OAuth2User;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CustomOAuth2LoginAuthenticationProvider implements AuthenticationProvider {

    private final OAuth2LoginAuthenticationProvider delegate;

    public CustomOAuth2LoginAuthenticationProvider(
            OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient,
            OAuth2UserService<OAuth2UserRequest, OAuth2User> userService) {
        this.delegate = new OAuth2LoginAuthenticationProvider(accessTokenResponseClient, userService);
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        log.info("🔍 INTERCEPTED: OAuth2LoginAuthenticationProvider.authenticate()");

        OAuth2LoginAuthenticationToken loginAuthenticationToken = (OAuth2LoginAuthenticationToken) authentication;

        // log.info("Client Registration ID: {}", loginAuthenticationToken.getClientRegistration().getRegistrationId());
        // log.info("Authorization Exchange: {}", loginAuthenticationToken.getAuthorizationExchange());

        try {
            // Call delegate to perform actual authentication
            Authentication result = delegate.authenticate(authentication);

            // log.info("✅ OAuth2LoginAuthenticationProvider completed successfully");
            // log.info("Authenticated Principal: {}", result.getName());

            return result;

        } catch (Exception ex) {
            log.error("❌ OAuth2LoginAuthenticationProvider failed: {}", ex.getMessage());
            throw ex;
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return delegate.supports(authentication);
    }
}