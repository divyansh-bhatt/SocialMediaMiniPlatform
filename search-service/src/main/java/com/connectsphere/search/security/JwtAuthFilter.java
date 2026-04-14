package com.connectsphere.search.security;

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

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * All search and hashtag endpoints are public (guests can browse).
     * Internal indexing endpoints are called service-to-service with no JWT.
     * So we skip JWT processing for everything — search-service never
     * requires authentication on its own endpoints.
     *
     * We still process the JWT if present so that future role-based
     * admin endpoints can use request.getAttribute("role").
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        // Internal endpoints — no JWT from other services
        return path.startsWith("/search/internal/")
                || path.startsWith("/search/")
                || path.startsWith("/hashtags/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        String token = (StringUtils.hasText(header) && header.startsWith("Bearer "))
                ? header.substring(7) : null;

        if (token != null && jwtUtil.validateToken(token)) {
            int userId    = jwtUtil.getUserIdFromToken(token);
            String username = jwtUtil.getUsernameFromToken(token);
            String role   = jwtUtil.getRoleFromToken(token);

            request.setAttribute("userId", userId);
            request.setAttribute("role", role);

            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                            username, null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    )
            );
        }

        filterChain.doFilter(request, response);
    }
}
