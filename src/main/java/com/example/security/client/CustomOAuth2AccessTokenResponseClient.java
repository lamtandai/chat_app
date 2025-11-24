package com.example.security.client;


import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CustomOAuth2AccessTokenResponseClient
        implements OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> {

    private final DefaultAuthorizationCodeTokenResponseClient delegate = new DefaultAuthorizationCodeTokenResponseClient();
    
    @Override
    public OAuth2AccessTokenResponse getTokenResponse(
            OAuth2AuthorizationCodeGrantRequest authorizationCodeGrantRequest) {

        log.info("🔍 INTERCEPTED: OAuth2AuthorizationCodeAuthenticationProvider - Token Exchange");
        log.info("Provider: {}", authorizationCodeGrantRequest.getClientRegistration().getRegistrationId());
        log.info("Authorization Code: {}",
                authorizationCodeGrantRequest.getAuthorizationExchange().getAuthorizationResponse().getCode());
        log.info("Token Endpoint: {}",
                authorizationCodeGrantRequest.getClientRegistration().getProviderDetails().getTokenUri());
        
        log.info("OAuth2AuthorizationCodeGrantRequest");
        // log.info("Code: {}",  authorizationCodeGrantRequest.getAuthorizationExchange().getAuthorizationResponse().getCode());
        log.info("Redirect URI: {}", authorizationCodeGrantRequest.getAuthorizationExchange().getAuthorizationResponse().getRedirectUri());
        log.info("State: {}", authorizationCodeGrantRequest.getAuthorizationExchange().getAuthorizationResponse().getState());
        log.info("Scope: {}", authorizationCodeGrantRequest.getAuthorizationExchange().getAuthorizationRequest().getScopes());
        log.info("AuthorizationUri: {}", authorizationCodeGrantRequest.getAuthorizationExchange().getAuthorizationRequest().getAuthorizationUri());
        
        try {
            // Call delegate to perform actual token exchange
            OAuth2AccessTokenResponse tokenResponse = delegate.getTokenResponse(authorizationCodeGrantRequest);

            log.info("✅ Token Exchange Successful");
            log.info("Access Token Type: {}", tokenResponse.getAccessToken().getTokenType().getValue());
            log.info("Scopes: {}", tokenResponse.getAccessToken().getScopes());
            log.info("Expires In: {} seconds", tokenResponse.getAccessToken().getExpiresAt());

            // You can modify the token response here if needed
            return tokenResponse;

        } catch (Exception ex) {
            log.error("❌ Token Exchange Failed: {}", ex.getMessage());
            throw ex;
        }
    }
}