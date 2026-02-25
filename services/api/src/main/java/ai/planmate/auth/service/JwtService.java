package ai.planmate.auth.service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ai.planmate.auth.entity.AppUser;
import ai.planmate.auth.entity.AuthProvider;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

/** JWT token generation and validation service */
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access-token-expiration-ms:900000}") // 15 minutes default
    private long accessTokenExpirationMs;

    @Value("${jwt.refresh-token-expiration-ms:2592000000}") // 30 days default
    private long refreshTokenExpirationMs;

    /** Generate access token for user */
    public String generateAccessToken(AppUser user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", user.getEmail());
        claims.put("provider", user.getProvider().name());
        return createToken(claims, user.getId().toString(), accessTokenExpirationMs);
    }

    /** Generate refresh token for user */
    public String generateRefreshToken(AppUser user) {
        return createToken(new HashMap<>(), user.getId().toString(), refreshTokenExpirationMs);
    }

    /** Create JWT token with claims and expiration */
    private String createToken(Map<String, Object> claims, String subject, long expirationMs) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /** Extract user ID from token */
    public UUID extractUserId(String token) {
        String userId = extractClaim(token, Claims::getSubject);
        return UUID.fromString(userId);
    }

    /** Extract email from token */
    public String extractEmail(String token) {
        return extractClaim(token, claims -> claims.get("email", String.class));
    }

    /** Extract provider from token */
    public AuthProvider extractProvider(String token) {
        String provider = extractClaim(token, claims -> claims.get("provider", String.class));
        return AuthProvider.valueOf(provider);
    }

    /** Extract expiration date from token */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /** Extract specific claim from token */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /** Extract all claims from token */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** Check if token is expired */
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /** Validate token */
    public Boolean validateToken(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    /** Get signing key from secret */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
