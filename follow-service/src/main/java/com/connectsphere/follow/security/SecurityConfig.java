package com.connectsphere.follow.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Autowired private JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(c -> c.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                // ── INTERNAL — called by post-service to get followee IDs for feed ──
                // GET /follows/{userId}/following-ids
                // No JWT needed — internal service-to-service call
                .requestMatchers(HttpMethod.GET, "/follows/*/following-ids").permitAll()

                // ── PUBLIC READ ────────────────────────────────────────────────────
                .requestMatchers(HttpMethod.GET, "/follows/*/followers").permitAll()
                .requestMatchers(HttpMethod.GET, "/follows/*/following").permitAll()
                .requestMatchers(HttpMethod.GET, "/follows/*/follower-count").permitAll()
                .requestMatchers(HttpMethod.GET, "/follows/*/following-count").permitAll()
                .requestMatchers(HttpMethod.GET, "/follows/is-following").permitAll()

                // ── EVERYTHING ELSE needs JWT ───────────────────────────────────────
                // POST /follows/{followeeId}     (follow)
                // DELETE /follows/{followeeId}   (unfollow)
                // GET /follows/suggested         (suggestions — needs userId from token)
                // GET /follows/mutual            (mutual — needs both userIds)
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
