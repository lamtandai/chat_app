package com.example.security.interceptor;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CustomOAuth2LoginAuthenticationFilter extends OAuth2LoginAuthenticationFilter {

    public CustomOAuth2LoginAuthenticationFilter(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientRepository authorizedClientRepository) {
        super(clientRegistrationRepository, authorizedClientRepository, "/login/oauth2/code/*");
        log.info("CustomOAuth2LoginAuthenticationFilter initialized");
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {

        log.info("🔍 INTERCEPTED: OAuth2LoginAuthenticationFilter.attemptAuthentication()");
        log.info("Request URI: {}", request.getRequestURI());
        log.info("Authorization Code: {}", request.getParameter("code"));
        log.info("State Parameter: {}", request.getParameter("state"));

        // You can add custom logic here before calling parent
        try {
            // Call the parent method to continue normal flow
            Authentication authentication = super.attemptAuthentication(request, response);

            log.info("✅ Authentication successful: {}", authentication.getName());
            return authentication;

        } catch (OAuth2AuthenticationException ex) {
            log.error("❌ OAuth2 Authentication failed: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("❌ General authentication error: {}", ex.getMessage());
            throw ex;
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
            jakarta.servlet.FilterChain chain, Authentication authResult) throws IOException, ServletException {

        log.info("🎉 INTERCEPTED: Successful OAuth2 Authentication");
        log.info("Authenticated user: {}", authResult.getName());
        log.info("Authorities: {}", authResult.getAuthorities());

        // Call parent to continue normal flow
        super.successfulAuthentication(request, response, chain, authResult);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException failed) throws IOException, ServletException {

        log.error("💥 INTERCEPTED: Failed OAuth2 Authentication");
        log.error("Error: {}", failed.getMessage());

        // Call parent to continue normal flow
        super.unsuccessfulAuthentication(request, response, failed);
    }
}