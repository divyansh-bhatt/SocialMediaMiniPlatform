package com.connectsphere.comment.security;

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
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        // Skip JWT for internal endpoints — called service-to-service with no token
        return path.startsWith("/comments/internal/")
                || path.startsWith("/comments/post/")
                || path.startsWith("/comments/user/")
                || path.startsWith("/comments/count/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        String token = (StringUtils.hasText(header) && header.startsWith("Bearer "))
                ? header.substring(7) : null;
        System.out.println("JWT USER ID = " + jwtUtil.getUserIdFromToken(token));
        System.out.println("JWT USERNAME = " + jwtUtil.getUsernameFromToken(token));

        if (token != null && jwtUtil.validateToken(token)) {
            request.setAttribute("userId", jwtUtil.getUserIdFromToken(token));
            request.setAttribute("role",   jwtUtil.getRoleFromToken(token));
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                            jwtUtil.getUsernameFromToken(token), null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + jwtUtil.getRoleFromToken(token)))
                    )
            );
        }
        filterChain.doFilter(request, response);
    }

}
