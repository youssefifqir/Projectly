package com.example.Projectly.service.facade.security;

public interface JwtService {

    String generateAccessToken(String username);

    String generateRefreshToken(String username);

    String generateRefreshToken(String username, long expirationMs);

    boolean isTokenValid(String token, String expectedUsername);

    boolean isAccessToken(String token);

    boolean isRefreshToken(String token);

    String extractUsername(String token);

    String refreshAccessToken(String refreshToken);

    String extractJti(String token);

    long extractRemainingTtlSeconds(String token);
}
