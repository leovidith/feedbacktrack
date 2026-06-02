package com.cognizant.training.feedbacktrack.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct; 
import java.security.Key;
import java.util.Date;
import java.util.Base64;

@Component
public class JwtUtil {

    
    @Value("${jwt.secret}")
    private String secretString;

    private static Key key;
    private static final long EXPIRATION_TIME = 86400000; 

    
    @PostConstruct
    public void init() {
        
        key = Keys.hmacShaKeyFor(secretString.getBytes());
    }

    public static Key getKey() {
        return key;
    }

    public String generateToken(Long userId, String role) {
        return Jwts.builder()
                .setSubject(Long.toString(userId))
                .claim("role", role) 
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(key)
                .compact();
    }
}