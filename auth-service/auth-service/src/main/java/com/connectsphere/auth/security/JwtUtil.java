package com.connectsphere.auth.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;


    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateToken(int userId, String username, String role) {
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("username", username)
                .claim("userId", userId)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            throw new RuntimeException("Token has expired. Please log in again.");
        } catch (UnsupportedJwtException e) {
            throw new RuntimeException("Unsupported JWT token.");
        } catch (MalformedJwtException e) {
            throw new RuntimeException("Malformed JWT token.");
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("JWT token is empty or null.");
        }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public int getUserIdFromToken(String token) {
        return Integer.parseInt(extractAllClaims(token).getSubject());
    }

    public String getUsernameFromToken(String token) {
        return (String) extractAllClaims(token).get("username");
    }

    public String getRoleFromToken(String token) {
        return (String) extractAllClaims(token).get("role");
    }

    public Date getExpirationFromToken(String token) {
        return extractAllClaims(token).getExpiration();
    }

    public boolean isTokenExpired(String token) {
        return getExpirationFromToken(token).before(new Date());
    }
}
