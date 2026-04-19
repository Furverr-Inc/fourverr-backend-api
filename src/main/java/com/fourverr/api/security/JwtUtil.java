package com.fourverr.api.security;

import com.fourverr.api.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:86400000}") // 24 horas
    private int expiration;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    // EL MÉTODO QUE USAN LOS CONTROLADORES (Recibe User completo)
    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole().name());
        claims.put("userId", user.getId());
        // Agregamos el nombre mostrado para el frontend
        claims.put("nombreMostrado", user.getNombreMostrado()); 
        return createToken(claims, user.getUsername());
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * ID del usuario en el claim {@code userId} (tokens emitidos tras login).
     * Puede ser null en tokens antiguos que no incluyan el claim.
     */
    public Long extractUserId(String token) {
        try {
            Object v = extractClaim(token, c -> c.get("userId"));
            if (v == null) return null;
            if (v instanceof Long) return (Long) v;
            if (v instanceof Integer) return ((Integer) v).longValue();
            if (v instanceof Number) return ((Number) v).longValue();
            return Long.parseLong(v.toString());
        } catch (Exception e) {
            return null;
        }
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claimsResolver.apply(claims);
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !extractExpiration(token).before(new Date()));
    }
}