package com.example.security.interceptor;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class OAuth2AuthenticationInterceptor {

    /**
     * Intercepts and logs the complete OAuth2 authentication flow
     */
    public void logAuthenticationFlow(Authentication authentication) {
        if (authentication instanceof OAuth2LoginAuthenticationToken) {
            OAuth2LoginAuthenticationToken loginToken = (OAuth2LoginAuthenticationToken) authentication;

            log.info("=".repeat(80));
            log.info("🔍 OAUTH2 AUTHENTICATION FLOW INTERCEPTED");
            log.info("=".repeat(80));

            // 1. Client Registration Details
            log.info("📋 CLIENT REGISTRATION:");
            log.info("  Registration ID: {}", loginToken.getClientRegistration().getRegistrationId());
            log.info("  Client Name: {}", loginToken.getClientRegistration().getClientName());
            log.info("  Provider: {}",
                    loginToken.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUri());

            // 2. Authorization Exchange
            log.info("🔄 AUTHORIZATION EXCHANGE:");
            log.info("  Authorization Request State: {}",
                    loginToken.getAuthorizationExchange().getAuthorizationRequest().getState());
            log.info("  Authorization Code: {}",
                    loginToken.getAuthorizationExchange().getAuthorizationResponse().getCode());
            log.info("  Redirect URI: {}",
                    loginToken.getAuthorizationExchange().getAuthorizationRequest().getRedirectUri());

            // 3. Access Token
            if (loginToken.getAccessToken() != null) {
                log.info("🎫 ACCESS TOKEN:");
                log.info("  Token Type: {}", loginToken.getAccessToken().getTokenType().getValue());
                log.info("  Scopes: {}", loginToken.getAccessToken().getScopes());
                log.info("  Expires At: {}", loginToken.getAccessToken().getExpiresAt());
            }

            // 4. User Principal
            if (loginToken.getPrincipal() instanceof OAuth2User) {
                OAuth2User oAuth2User = (OAuth2User) loginToken.getPrincipal();
                log.info("👤 USER PRINCIPAL:");
                log.info("  Name: {}", oAuth2User.getName());
                log.info("  Authorities: {}", oAuth2User.getAuthorities());
                log.info("  Attributes: {}", oAuth2User.getAttributes().keySet());
            }

            log.info("=".repeat(80));
        }
    }

    /**
     * Log token exchange details
     */
    public void logTokenExchange(String registrationId, String authCode, String tokenEndpoint) {
        log.info("🔄 TOKEN EXCHANGE INITIATED:");
        log.info("  Provider: {}", registrationId);
        log.info("  Auth Code: {}...{}",
                authCode.substring(0, Math.min(10, authCode.length())),
                authCode.substring(Math.max(authCode.length() - 10, 0)));
        log.info("  Token Endpoint: {}", tokenEndpoint);
    }

    /**
     * Log user info retrieval
     */
    public void logUserInfoRetrieval(String userInfoUri, String accessToken) {
        log.info("👤 USER INFO RETRIEVAL:");
        log.info("  UserInfo URI: {}", userInfoUri);
        log.info("  Access Token: {}...{}",
                accessToken.substring(0, Math.min(10, accessToken.length())),
                accessToken.substring(Math.max(accessToken.length() - 10, 0)));
    }
}