package com.example.Projectly.service.impl.security;

import com.example.Projectly.common.util.KeyUtils;
import com.example.Projectly.service.facade.security.JwtService;
import com.example.Projectly.exception.BusinessException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static com.example.Projectly.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
public class JwtServiceImpl implements JwtService {

    public static final String TOKEN_TYPE = "token_type";
    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    
    @Value("${app.security.jwt.access-token-expiration}")
    private long accessTokenExpiration;
    
    @Value("${app.security.jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Override
    public String generateAccessToken(final String username) {
        final Map<String, Object> claims = Map.of(TOKEN_TYPE, "ACCESS_TOKEN");
        return buildToken(username, claims, this.accessTokenExpiration);
    }

    @Override
    public String generateRefreshToken(final String username) {
        final Map<String, Object> claims = Map.of(TOKEN_TYPE, "REFRESH_TOKEN");
        return buildToken(username, claims, this.refreshTokenExpiration);
    }

    @Override
    public String generateRefreshToken(final String username, final long expirationMs) {
        final Map<String, Object> claims = Map.of(TOKEN_TYPE, "REFRESH_TOKEN");
        return buildToken(username, claims, expirationMs);
    }

    private String buildToken(final String username, final Map<String, Object> claims, final long expiration) {
        final Map<String, Object> allClaims = new java.util.HashMap<>(claims);
        allClaims.put("jti", UUID.randomUUID().toString());
        return Jwts.builder()
                .claims(allClaims)
                .subject(username)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(this.privateKey)
                .compact();
    }

    @Override
    public boolean isTokenValid(final String token, final String expectedUsername) {
        final String username = extractUsername(token);
        return username.equals(expectedUsername) && !isTokenExpired(token);
    }

    @Override
    public String extractUsername(final String token) {
        return extractClaims(token).getSubject();
    }

    private boolean isTokenExpired(final String token) {
        return extractClaims(token).getExpiration()
                .before(new Date());
    }

    private Claims extractClaims(final String token) {
        try {
            return Jwts.parser()
                    .verifyWith(this.publicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (final JwtException e) {
            throw new BusinessException(TOKEN_INVALID);
        }
    }

    @Override
    public boolean isAccessToken(final String token) {
        return "ACCESS_TOKEN".equals(extractClaims(token).get(TOKEN_TYPE, String.class));
    }

    @Override
    public boolean isRefreshToken(final String token) {
        return "REFRESH_TOKEN".equals(extractClaims(token).get(TOKEN_TYPE, String.class));
    }

    @Override
    public String extractJti(final String token) {
        return extractClaims(token).get("jti", String.class);
    }

    @Override
    public long extractRemainingTtlSeconds(final String token) {
        final Date expiration = extractClaims(token).getExpiration();
        final long remaining = (expiration.getTime() - System.currentTimeMillis()) / 1000;
        return Math.max(remaining, 0);
    }

    @Override
    public String refreshAccessToken(final String refreshToken) {
        final Claims claims = extractClaims(refreshToken);

        if (!"REFRESH_TOKEN".equals(claims.get(TOKEN_TYPE, String.class))) {
            throw new BusinessException(TOKEN_INVALID);
        }
        if (claims.getExpiration().before(new Date())) {
            throw new BusinessException(TOKEN_EXPIRED);
        }

        final String username = claims.getSubject();
        return generateAccessToken(username);
    }
}
