package com.connectsphere.gateway;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * JwtAuthFilter — Spring Cloud Gateway filter factory.
 *
 * Applied to routes that need authentication (see application.yml).
 * For public routes the filter is not applied at all.
 *
 * What it does:
 *  1. Checks for Authorization: Bearer <token> header
 *  2. Validates the JWT signature and expiry
 *  3. Injects X-User-Id, X-Username, X-User-Role headers into the
 *     forwarded request so downstream microservices can trust them
 *     without re-validating the JWT themselves
 *  4. Returns 401 if token is missing or invalid
 *
 * NOTE: Spring Cloud Gateway is reactive (WebFlux).
 * This filter is a GatewayFilterFactory (not a WebFilter) because
 * it needs to be applied per-route in application.yml.
 */
@Component
public class JwtAuthFilter extends AbstractGatewayFilterFactory<JwtAuthFilter.Config> {

    @Autowired
    private JwtUtil jwtUtil;

    // Paths that never require a JWT even if the filter is applied
    private static final List<String> OPEN_PATHS = List.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/validate",
            "/api/auth/search",
            "/api/posts/public",
            "/api/posts/search",
            "/api/search/posts",
            "/api/search/users",
            "/api/search/hashtags",
            "/api/hashtags"
    );

    public JwtAuthFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();

            // Skip JWT check for public paths
            boolean isPublic = OPEN_PATHS.stream().anyMatch(path::startsWith);
            if (isPublic) {
                return chain.filter(exchange);
            }

            // Check Authorization header
            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return unauthorised(exchange, "Missing Authorization header");
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return unauthorised(exchange, "Invalid Authorization format. Use: Bearer <token>");
            }

            String token = authHeader.substring(7);

            if (!jwtUtil.validateToken(token)) {
                return unauthorised(exchange, "Token is invalid or expired");
            }

            // Inject user info as headers so downstream services don't need to re-validate
            int userId      = jwtUtil.getUserId(token);
            String username = jwtUtil.getUsername(token);
            String role     = jwtUtil.getRole(token);

            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id",   String.valueOf(userId))
                    .header("X-Username",  username)
                    .header("X-User-Role", role)
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        };
    }

    private Mono<Void> unauthorised(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json");
        var body = response.bufferFactory()
                .wrap(("{\"error\":\"" + message + "\"}").getBytes());
        return response.writeWith(Mono.just(body));
    }

    public static class Config {
        // No config fields needed for now
    }
}
