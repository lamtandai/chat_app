package com.example.security.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.security.interceptor.OAuth2AuthenticationInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AuthTestController {

    private final OAuth2AuthenticationInterceptor authInterceptor;
            // Log the complete authentication flow
            
    @GetMapping("/api/home")
    public String home(Authentication authentication) {
        if (authentication != null) {
            // Log the complete authentication flow
            authInterceptor.logAuthenticationFlow(authentication);

            if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
                return "Welcome " + oauth2User.getAttribute("name") + "!";
            }
            return "Welcome " + authentication.getName() + "!";
        }
        return "Please login";
    }

    @GetMapping("/user")
    public Object user(Authentication authentication) {
        log.info("Current Authentication: {}", authentication);
        return authentication.getPrincipal();
    }

    /**
     * Reset OAuth2 login process by clearing session
     */
    @PostMapping("/logout")
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        log.info("🚪 Logging out user: {}", authentication != null ? authentication.getName() : "anonymous");

        if (authentication != null) {
            // Clear Spring Security context and session
            new SecurityContextLogoutHandler().logout(request, response, authentication);
        }

        // Clear session
        if (request.getSession(false) != null) {
            request.getSession().invalidate();
        }

        log.info("✅ Logout completed");
    }

    /**
     * Get logout endpoint for convenience
     */
    /**
     * Clear security context manually
     */
    @PostMapping("/clear-session")
    public String clearSession(HttpServletRequest request) {
        log.info("🧹 Clearing security context and session");

        // Clear Spring Security context
        SecurityContextHolder.clearContext();

        // Invalidate session
        if (request.getSession(false) != null) {
            request.getSession().invalidate();
        }

        return "Security context and session cleared";
    }


}