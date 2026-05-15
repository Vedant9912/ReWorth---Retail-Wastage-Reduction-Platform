package com.reworth.security.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service @Slf4j
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    public String generateToken(UserDetails user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getAuthorities().iterator().next().getAuthority());
        return build(claims, user.getUsername(), expirationMs);
    }

    public String generateRefreshToken(UserDetails user) {
        return build(new HashMap<>(), user.getUsername(), refreshExpirationMs);
    }

    private String build(Map<String, Object> claims, String subject, long expMs) {
        return Jwts.builder()
            .claims(claims).subject(subject)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expMs))
            .signWith(key())
            .compact();
    }

    public boolean isValid(String token, UserDetails user) {
        return extractUsername(token).equals(user.getUsername()) && !isExpired(token);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    public <T> T extractClaim(String token, Function<Claims, T> fn) {
        return fn.apply(Jwts.parser().verifyWith(key()).build()
            .parseSignedClaims(token).getPayload());
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }
}
