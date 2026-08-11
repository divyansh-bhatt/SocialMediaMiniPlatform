package com.connectsphere.search.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.security.Key;

@Component
public class JwtUtil {
    @Value("${jwt.secret}")
    private String jwtSecret;

    private Key getSigningKey() { return Keys.hmacShaKeyFor(jwtSecret.getBytes()); }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) { return false; }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody();
    }

    public int getUserIdFromToken(String token) { return Integer.parseInt(extractAllClaims(token).getSubject()); }
    public String getUsernameFromToken(String token) { return (String) extractAllClaims(token).get("username"); }
    public String getRoleFromToken(String token) { return (String) extractAllClaims(token).get("role"); }
}
