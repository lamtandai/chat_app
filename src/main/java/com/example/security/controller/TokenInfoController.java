package com.example.security.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.security.dto.UserPrincipal;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/token")
public class TokenInfoController {

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getTokenInfo(
            Authentication authentication,
            HttpServletRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        Map<String, Object> info = new HashMap<>();

        // Check authentication
        if (authentication != null && authentication.isAuthenticated()) {
            info.put("authenticated", true);
            info.put("username", authentication.getName());
            info.put("authorities", authentication.getAuthorities());

            if (authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
                info.put("userId", userPrincipal.getId());
                info.put("provider", userPrincipal.getProvider());
                info.put("picture", userPrincipal.getPicture());
            }
        } else {
            info.put("authenticated", false);
        }

        // Check Authorization header
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            info.put("tokenInHeader", true);
            info.put("tokenPreview", authHeader.substring(0, Math.min(50, authHeader.length())) + "...");
        } else {
            info.put("tokenInHeader", false);
        }

        // Check cookies
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("access_token".equals(cookie.getName())) {
                    info.put("tokenInCookie", true);
                    info.put("cookieTokenPreview",
                            cookie.getValue().substring(0, Math.min(50, cookie.getValue().length())) + "...");
                    break;
                }
            }
        }

        return ResponseEntity.ok(info);
    }
}
