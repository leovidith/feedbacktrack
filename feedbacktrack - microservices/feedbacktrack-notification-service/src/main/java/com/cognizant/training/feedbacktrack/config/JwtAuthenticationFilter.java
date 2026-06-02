package com.cognizant.training.feedbacktrack.config;

import com.cognizant.training.feedbacktrack.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();

            System.out.println("Token received: [" + token + "]");

            Claims claims = jwtUtil.validate(token);

            System.out.println("Claims: " + claims);

            if (claims != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                String subject = claims.getSubject();
                String role = claims.get("role", String.class);

                if (subject != null && role != null) {
                    Long userId = Long.parseLong(subject);

                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(
                                    userId,
                                    null,
                                    List.of(new SimpleGrantedAuthority("ROLE_" + role))
                            );

                    SecurityContextHolder.getContext().setAuthentication(auth);
                    System.out.println("Authenticated userId: " + userId + " with role: " + role);
                } else {
                    System.err.println("Claims missing subject or role — subject: " + subject + ", role: " + role);
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}