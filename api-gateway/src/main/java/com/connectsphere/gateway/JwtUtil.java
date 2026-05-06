package com.connectsphere.gateway;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public int getUserId(String token) {
        try {
            Claims claims = extractAllClaims(token);

            // Try "userId" claim (Integer)
            Object userIdClaim = claims.get("userId");
            if (userIdClaim instanceof Integer) return (Integer) userIdClaim;
            if (userIdClaim instanceof Long)    return ((Long) userIdClaim).intValue();
            if (userIdClaim instanceof Number)  return ((Number) userIdClaim).intValue();

            // Try "user_id" claim (snake_case fallback)
            Object userIdSnake = claims.get("user_id");
            if (userIdSnake instanceof Integer) return (Integer) userIdSnake;
            if (userIdSnake instanceof Long)    return ((Long) userIdSnake).intValue();
            if (userIdSnake instanceof Number)  return ((Number) userIdSnake).intValue();

            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public String getUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String getRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    public boolean isExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }
}
