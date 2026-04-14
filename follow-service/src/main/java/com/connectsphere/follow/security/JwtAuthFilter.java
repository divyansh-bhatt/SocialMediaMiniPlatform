package com.connectsphere.follow.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    @Autowired private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        String token = (StringUtils.hasText(header) && header.startsWith("Bearer "))
                       ? header.substring(7) : null;
        if (token != null && jwtUtil.validateToken(token)) {
            req.setAttribute("userId", jwtUtil.getUserIdFromToken(token));
            req.setAttribute("role",   jwtUtil.getRoleFromToken(token));
            SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    jwtUtil.getUsernameFromToken(token), null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + jwtUtil.getRoleFromToken(token)))
                )
            );
        }
        chain.doFilter(req, res);
    }
}
