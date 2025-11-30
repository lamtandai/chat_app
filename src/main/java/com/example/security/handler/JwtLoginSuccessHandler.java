package com.example.security.handler;

import com.example.security.dto.UserPrincipal;
import com.example.security.model.User;
import com.example.security.model.Authority.Token;
import com.example.security.repository.TokenRepository;
import com.example.security.repository.UserRepository;
import com.example.security.service.JwtService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtLoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
            throws IOException, ServletException {

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findByUsername(userPrincipal.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 🧩 Check for existing valid access token (reuse if possible)
        String existingAccess = getCookieValue(request, "access_token");
        boolean reuseToken = existingAccess != null && jwtService.isTokenValid(existingAccess, user);

        String accessToken = existingAccess;
        String refreshToken = getCookieValue(request, "refresh_token");

        if (!reuseToken) {
            accessToken = jwtService.generateToken(user);
            refreshToken = jwtService.generateRefreshToken(user);
            
            Token refresh_token = Token
                .builder()
                .user(user)
                .token(refreshToken)
                .expired(false)
                .revoked(false)
                .build();
            tokenRepository.save(refresh_token);

            log.info("🔑 Generated new JWT tokens for user {}", user.getUsername());
        } else {
            log.info("♻️ Reusing existing valid access token for {}", user.getUsername());
        }

        // 🍪 Save cookies (even if reusing)
        setCookie(response, "access_token", accessToken, 24 * 3600);

        // 🧭 Determine where to go next

        // ⚙️ Internal forward — NOT external redirect
        if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            response.sendRedirect("/api/v1/admin");
        } else {
            response.sendRedirect("/chat");

        }
    }

    private String getCookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() == null)
            return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> name.equals(c.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElse(null);
    }

    private void setCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // set true in production
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }
}
