package com.cognizant.training.feedbacktrack.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    private static final String secret_key = "FeedbackTrackProjectSecretKey2026!";
    private final Key key= Keys.hmacShaKeyFor(secret_key.getBytes());


    public Long extractId(String jwt) {
        return Long.parseLong(extractAllClaims(jwt).getSubject());
    }

    public boolean isTokenValid(String jwt) {
        try{
            Claims claims=extractAllClaims(jwt);
            Date expiration=claims.getExpiration();
            if(expiration.after(new Date())){
                return true;
            }
            return false;
        }
        catch (Exception e){
            System.out.println("JWT validation failed: " + e.getMessage());
            return false;
        }
    }

    private Claims extractAllClaims(String jwt) {
        Claims claims =  Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(jwt)
                .getBody();
        return claims;
    }
}
