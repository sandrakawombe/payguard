package com.payguard.user.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Step 1: Extract token from the Authorization header
        String token = extractToken(request);

        // Step 2: If token exists and is valid, set authentication
        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {

            UUID userId = jwtTokenProvider.getUserIdFromToken(token);
            String role = jwtTokenProvider.getRoleFromToken(token);

            // Create an authentication object with the user's role
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId,                                          // principal (who)
                            null,                                            // credentials (not needed, token already validated)
                            List.of(new SimpleGrantedAuthority("ROLE_" + role)) // authorities (what they can do)
                    );

            // Tell Spring Security: "This request is authenticated"
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // Step 3: Continue to the next filter / controller
        filterChain.doFilter(request, response);
    }

    /**
     * Extract Bearer token from Authorization header.
     * Header format: "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}