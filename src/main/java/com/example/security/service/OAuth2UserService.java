package com.example.security.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.example.security.dto.Oauth2UserInfoDto;
import com.example.security.dto.RegisterRequest;
import com.example.security.dto.UserPrincipal;
import com.example.security.interceptor.OAuth2AuthenticationInterceptor;
import com.example.security.model.User;
import com.example.security.repository.UserRepository;
import com.example.security.sdk.processOAuth2User.OAuth2Provider;
import com.example.security.sdk.processOAuth2User.OAuth2ProviderFactory;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class OAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final AuthenticationService authenticationService;

    @Autowired
    private OAuth2AuthenticationInterceptor authInterceptor;

    // Constructor with @Lazy to break circular dependency
    public OAuth2UserService(UserRepository userRepository,
            @Lazy AuthenticationService authenticationService) {
        this.userRepository = userRepository;
        this.authenticationService = authenticationService;
    }

    @Override
    @SneakyThrows
    public OAuth2User loadUser(OAuth2UserRequest oAuth2UserRequest) {
        log.info("🔍 INTERCEPTED: OAuth2UserService.loadUser()");

        // Log detailed request information
        authInterceptor.logUserInfoRetrieval(
                oAuth2UserRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUri(),
                oAuth2UserRequest.getAccessToken().getTokenValue());

        // Call parent to fetch user info from provider
        OAuth2User oAuth2User = super.loadUser(oAuth2UserRequest);
        // Process and return custom user
        return processOAuth2User(oAuth2UserRequest, oAuth2User);
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest oAuth2UserRequest, OAuth2User oAuth2User) {
        String registrationId = oAuth2UserRequest.getClientRegistration().getRegistrationId();
        OAuth2Provider provider = OAuth2ProviderFactory.getProvider(registrationId);
        Oauth2UserInfoDto userInfoDto = provider.extractUserInfo(oAuth2User.getAttributes());

        // Use email for lookup, but ensure it's not null
        String rawEmail = userInfoDto.getEmail();
        String lookupEmail;
        if (rawEmail == null || rawEmail.trim().isEmpty()) {
            lookupEmail = userInfoDto.getId() + "@" + registrationId + ".local";
            log.warn("⚠️ Email was null/empty, using generated email for lookup: {}", lookupEmail);
        } else {
            lookupEmail = rawEmail;
        }

        Optional<User> userOptional = userRepository.findByEmail(lookupEmail)
                .or(() -> userRepository.findByUsername(lookupEmail));
        User user = userOptional
                .map(existingUser -> {
                    log.info("👤 Found existing user: {}", existingUser.getUsername());
                    return updateExistingUser(existingUser, userInfoDto);
                })
                .orElseGet(() -> {
                    log.info("🆕 Creating new user");
                    return registerNewUser(oAuth2UserRequest, userInfoDto);
                });

        UserPrincipal userPrincipal = UserPrincipal.create(user, oAuth2User.getAttributes());
    
        // Validate that the principal name is not null or empty
        if (userPrincipal.getName() == null || userPrincipal.getName().trim().isEmpty()) {
            throw new IllegalStateException("Principal name cannot be null or empty");
        }

        return userPrincipal;
    }

    private User registerNewUser(OAuth2UserRequest oAuth2UserRequest, Oauth2UserInfoDto userInfoDto) {
        String providerName = oAuth2UserRequest.getClientRegistration().getRegistrationId();

        // Extract first name and last name from the full name
        String fullName = userInfoDto.getName();
        String firstname = "";
        String lastname = "";

        if (fullName != null && !fullName.trim().isEmpty()) {
            String[] nameParts = fullName.trim().split("\\s+", 2);
            firstname = nameParts[0];
            lastname = nameParts.length > 1 ? nameParts[1] : "";
        }

        // Ensure email is never null or empty
        String email = userInfoDto.getEmail();
        if (email == null || email.trim().isEmpty()) {
            email = userInfoDto.getId() + "@" + providerName + ".local";
            log.warn("⚠️ Email was null/empty, using generated email: {}", email);
        }

        RegisterRequest registerRequest = RegisterRequest.builder()
                .firstname(firstname)
                .lastname(lastname)
                .email(email)
                .password(null) // OAuth2 users don't have passwords
                .role(com.example.security.model.Authority.Role.USER)
                .build();

        // Call the registration service (same as basic auth flow)
        User savedUser = authenticationService.registerUserFromOAuth2(registerRequest, providerName,
                userInfoDto.getId(),
                userInfoDto.getPicture());

        return savedUser;
    }

    private User updateExistingUser(User existingUser, Oauth2UserInfoDto userInfoDto) {
        // Update name and picture from OAuth2 provider
        String fullName = userInfoDto.getName();
        existingUser.setName(fullName);
        existingUser.setPicture(userInfoDto.getPicture());

        String email = userInfoDto.getEmail();
        if (email != null && !email.trim().isEmpty()) {
            existingUser.setEmail(email);
            existingUser.setUsername(email);
        }

        // Update first name and last name if the full name has changed
        if (fullName != null && !fullName.trim().isEmpty()) {
            String[] nameParts = fullName.trim().split("\\s+", 2);
            existingUser.setFirstname(nameParts[0]);
            existingUser.setLastname(nameParts.length > 1 ? nameParts[1] : "");
        }

        return userRepository.save(existingUser);
    }

}
