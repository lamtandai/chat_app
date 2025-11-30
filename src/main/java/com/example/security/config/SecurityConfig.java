package com.example.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutHandler;

import com.example.security.filter.JwtAuthenticationFilter;
import com.example.security.handler.JwtLoginSuccessHandler;
import com.example.security.resolver.CustomAuthorizationRequestResolver;
import com.example.security.service.OAuth2UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private static final String[] WHITE_LIST_URL = {
                        "/favicon.ico",
                        "/ws/**",
                        "/test/**",
                        "/api/v1/auth/**",
                        "/login",
                        "/login.html",
                        "/css/**",
                        "/js/**",
                        "/chat/**",
                        "/v2/api-docs",
                        "/v3/api-docs",
                        "/v3/api-docs/**",
                        "/swagger-resources",
                        "/swagger-resources/**",
                        "/configuration/ui",
                        "/configuration/security",
                        "/swagger-ui/**",
                        "/webjars/**",
                        "/swagger-ui.html"
        };

        private final OAuth2UserService oAuth2UserService;
        private final JwtAuthenticationFilter jwtAuthFilter;
        private final AuthenticationProvider authenticationProvider;
        private final LogoutHandler logoutHandler;
        private final JwtLoginSuccessHandler JwtLoginSuccessHandler;
        private final CustomAuthorizationRequestResolver customAuthorizationRequestResolver;

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                log.info("🔧 Configuring Unified Security Filter Chain (OAuth2 + JWT)");

                http
                                .csrf(AbstractHttpConfigurer::disable)
                                // Authorization rules
                                .authorizeHttpRequests(req -> req
                                                .requestMatchers(WHITE_LIST_URL).permitAll()
                                                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                                                .requestMatchers("/api/v1/users/**").hasRole("USER")
                                                .anyRequest().authenticated())

                                // Stateless session for JWT
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .formLogin(form -> form
                                                .loginPage("/login")
                                                .successHandler(JwtLoginSuccessHandler)
                                                .failureUrl("/login?error=true"))
                                // OAuth2 Login configuration
                                .oauth2Login(oauth2 -> oauth2
                                                .loginPage("/login")
                                                .authorizationEndpoint(auth -> auth
                                                                .authorizationRequestResolver(
                                                                                customAuthorizationRequestResolver))
                                                .userInfoEndpoint(infoEndpoint -> infoEndpoint
                                                                .userService(oAuth2UserService))
                                                .successHandler(JwtLoginSuccessHandler)
                                                // SUCCESS: After OAuth2 login, generate JWT token

                                                .failureUrl("/login?error=true"))

                                // Form login (optional - if you want username/password login page)
                                // For API-based login, use /api/v1/auth/authenticate endpoint instead

                                // JWT Authentication filter
                                .authenticationProvider(authenticationProvider)

                                // Logout configuration
                                .logout(logout -> logout
                                                .logoutUrl("/logout")
                                                .addLogoutHandler(logoutHandler)
                                                .logoutSuccessHandler((request, response, authentication) -> {
                                                        SecurityContextHolder.clearContext();
                                                        response.sendRedirect("/login");
                                                })
                                                .deleteCookies("access_token", "refresh_token", "JSESSIONID"));

                http
                                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }
}
