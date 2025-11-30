package com.example.security.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.security.dto.AuthenticationRequest;
import com.example.security.dto.AuthenticationResponse;
import com.example.security.dto.RegisterRequest;
import com.example.security.service.AuthenticationService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

  private final AuthenticationService authenticationService;

  @PostMapping("/register")
  public ResponseEntity<AuthenticationResponse> register(
      @RequestBody RegisterRequest request,
      HttpServletResponse response) {

    AuthenticationResponse authResponse = authenticationService.registerUser(request);

    addTokenCookie(response, authResponse.getAccessToken());

    
    return ResponseEntity.ok(authResponse);
  }

  @PostMapping("/authenticate")
  public ResponseEntity<AuthenticationResponse> authenticate(
      @RequestBody AuthenticationRequest request,
      HttpServletResponse response) {

    log.info("🔐 Authentication request for email: {}", request.getEmail());
    AuthenticationResponse authResponse = authenticationService.authenticate(request);

    // Add JWT tokens to cookies
    addTokenCookie(response, authResponse.getAccessToken());

    log.info("✅ User authenticated successfully, JWT tokens set in cookies");
    return ResponseEntity.ok(authResponse);
  }

  /**
   * Add JWT tokens to HTTP-only cookies for secure storage
   */
  private void addTokenCookie(HttpServletResponse response, String accessToken) {
    // Access Token Cookie
    Cookie accessTokenCookie = new Cookie("access_token", accessToken);
    accessTokenCookie.setHttpOnly(true);
    accessTokenCookie.setSecure(false); // Set to true in production with HTTPS
    accessTokenCookie.setPath("/");
    accessTokenCookie.setMaxAge(24 * 60 * 60); // 24 hours
    response.addCookie(accessTokenCookie);

    log.debug("🍪 JWT access token added to HTTP-only cookie");
  }
}
