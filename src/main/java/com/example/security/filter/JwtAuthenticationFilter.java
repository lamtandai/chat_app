package com.example.security.filter;

import java.io.IOException;
import java.util.Objects;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.security.dto.UserPrincipal;
import com.example.security.model.User;
import com.example.security.service.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtService jwtService;
  private final UserDetailsService userDetailsService;

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain) throws ServletException, IOException {

    // Skip JWT validation for auth endpoints
    if (request.getServletPath().contains("/api/v1/auth") ||
        request.getServletPath().contains("/login") ||
        request.getServletPath().contains("/oauth2")) {
      filterChain.doFilter(request, response);
      return;
    }

    Cookie[] cookies = request.getCookies();
    if (Objects.isNull(cookies)) {
      filterChain.doFilter(request, response);
      return;
    }

    String jwt = null;
    String userEmail = null;

    for (Cookie cookie : cookies) {
      if ("access_token".equals(cookie.getName())) {
        jwt = cookie.getValue();
        break;
      }
    }

    if (jwt == null || jwt.isBlank()) {
      filterChain.doFilter(request, response);
      return;
    }

    try {
      userEmail = jwtService.extractUsername(jwt);
    } catch (Exception ex) {

      filterChain.doFilter(request, response);
      return;
    }

    if (userEmail == null) {
      filterChain.doFilter(request, response);
      return;
    }

    Authentication existingAuth = SecurityContextHolder.getContext().getAuthentication();
    if (existingAuth != null && existingAuth instanceof UsernamePasswordAuthenticationToken) {
      filterChain.doFilter(request, response);
      return;
    }

    UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

    // Convert to UserPrincipal if it's a User entity
    UserPrincipal userPrincipal;
    if (userDetails instanceof UserPrincipal) {
      userPrincipal = (UserPrincipal) userDetails;
    } else if (userDetails instanceof User) {
      userPrincipal = UserPrincipal.create((User) userDetails);
    } else {
      filterChain.doFilter(request, response);
      return;
    }

    log.info("userPrincipal: {}", userPrincipal);

    if (jwtService.isTokenValid(jwt, userPrincipal)) {
      UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
          userPrincipal,
          null,
          userPrincipal.getAuthorities());
      authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

      SecurityContextHolder.getContext().setAuthentication(authToken);
    }

    filterChain.doFilter(request, response);

  }
}
