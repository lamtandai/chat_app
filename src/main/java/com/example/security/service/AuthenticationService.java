package com.example.security.service;

import java.util.UUID;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.security.dto.AuthenticationRequest;
import com.example.security.dto.AuthenticationResponse;
import com.example.security.dto.RegisterRequest;
import com.example.security.model.Role;
import com.example.security.model.User;
import com.example.security.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
  private final UserRepository repository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final AuthenticationManager authenticationManager;

  public AuthenticationResponse register(User user) {
    User savedUser = repository.save(user);
    return buildAuthenticationResponse(savedUser);
  }

  public AuthenticationResponse registerUser(RegisterRequest request) {
    User user = User.builder()
        .id(UUID.randomUUID())
        .firstname(request.getFirstname())
        .lastname(request.getLastname())
        .name(request.getFirstname() + " " + request.getLastname())
        .email(request.getEmail())
        .username(request.getEmail()) // Use email as username for consistency
        .password(passwordEncoder.encode(request.getPassword()))
        .role(Role.USER)
        .enabled(true)
        .accountNonExpired(true)
        .accountNonLocked(true)
        .credentialsNonExpired(true)
        .provider("local") // Mark as local registration (not OAuth2)
        .build();
    return register(user);
  }

  /**
   * 🎯 Register user from OAuth2 login (Google, Facebook, GitHub)
   * This method is called by OAuth2UserService after OAuth2 authentication
   * It uses the SAME RegisterRequest structure as basic auth registration!
   */
  public User registerUserFromOAuth2(RegisterRequest request, String provider, String providerId, String picture) {
    User user = User.builder()
        .id(UUID.randomUUID())
        .firstname(request.getFirstname())
        .lastname(request.getLastname())
        .name(request.getFirstname() + " " + request.getLastname())
        .email(request.getEmail())
        .username(request.getEmail()) // Use email as username (same as basic auth)
        .password(null) // OAuth2 users don't have passwords
        .role(request.getRole() != null ? request.getRole() : Role.USER)
        .enabled(true)
        .accountNonExpired(true)
        .accountNonLocked(true)
        .credentialsNonExpired(true)
        .provider(provider) // e.g., "google", "facebook", "github"
        .providerId(providerId) // OAuth2 provider's user ID
        .picture(picture) // Profile picture URL from OAuth2 provider
        .build();

    return repository.save(user);
  }

  public AuthenticationResponse registerAdmin(RegisterRequest request) {
    User user = User.builder()
        .id(UUID.randomUUID())
        .firstname(request.getFirstname())
        .lastname(request.getLastname())
        .name(request.getFirstname() + " " + request.getLastname())
        .email(request.getEmail())
        .username(request.getEmail()) // Use email as username for consistency
        .password(passwordEncoder.encode(request.getPassword()))
        .role(Role.ADMIN)
        .enabled(true)
        .accountNonExpired(true)
        .accountNonLocked(true)
        .credentialsNonExpired(true)
        .provider("local") // Mark as local registration (not OAuth2)
        .build();
    return register(user);
  }

  public AuthenticationResponse authenticate(AuthenticationRequest request) {
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
            request.getEmail(),
            request.getPassword()));
    User user = repository.findByEmail(request.getEmail())
        .orElseThrow();
    return buildAuthenticationResponse(user);
  }

  /**
   * Generate a fresh JWT for an existing user without touching persistence.
   */
  public AuthenticationResponse generateTokensForExistingUser(User user) {
    return buildAuthenticationResponse(user);
  }

  private AuthenticationResponse buildAuthenticationResponse(User user) {
    String accessToken = jwtService.generateToken(user);
    return AuthenticationResponse.builder()
        .accessToken(accessToken)
        .build();
  }
}
