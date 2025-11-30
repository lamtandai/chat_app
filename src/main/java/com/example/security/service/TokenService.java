package com.example.security.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.security.dto.AuthenticationResponse;
import com.example.security.model.User;
import com.example.security.model.Authority.Token;
import com.example.security.model.Authority.TokenType;
import com.example.security.repository.TokenRepository;

import lombok.RequiredArgsConstructor;

/**
 * Centralizes JWT generation and token persistence so multiple authentication
 * flows (basic, registration, OAuth2) can share consistent behaviour without
 * introducing unnecessary bean dependencies.
 */
@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtService jwtService;
    private final TokenRepository tokenRepository;

    public AuthenticationResponse generateTokensForUser(User user) {
        String accessToken = jwtService.generateToken(user);
        
        revokeAllUserTokens(user);
        saveUserToken(user, accessToken);
        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .build();
    }

    public void revokeAllUserTokens(User user) {
        List<Token> validUserTokens = tokenRepository.findAllValidTokenByUser(user.getId());
        if (validUserTokens.isEmpty()) {
            return;
        }
        validUserTokens.forEach(token -> {
            token.setExpired(true);
            token.setRevoked(true);
        });
        tokenRepository.saveAll(validUserTokens);
    }

    public void saveUserToken(User user, String jwtToken) {
        Token token = Token.builder()
                .user(user)
                .token(jwtToken)
                .tokenType(TokenType.BEARER)
                .expired(false)
                .revoked(false)
                .build();
        tokenRepository.save(token);
    }
}
