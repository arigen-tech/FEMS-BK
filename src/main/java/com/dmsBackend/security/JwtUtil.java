package com.dmsBackend.security;

import com.dmsBackend.entity.Employee;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    // ==================== OLD METHODS (Keep for backward compatibility) ====================
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody();
    }

    public Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    // ==================== NEW METHODS FOR DEVICE-BOUND TOKENS ====================

    // Extract deviceId from token
    public String extractDeviceId(String token) {
        return extractClaim(token, claims -> claims.get("deviceId", String.class));
    }

    // Extract JTI from token
    public String extractJti(String token) {
        return extractClaim(token, Claims::getId);
    }

    // Extract role from token
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    // Generate Access Token (10 minutes)
    public String generateAccessToken(Employee emp, String deviceId, String jti) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("deviceId", deviceId);
        claims.put("role", emp.getRole().getRole());
        claims.put("name", emp.getName());
        claims.put("employeeId", emp.getId());
        claims.put("type", "ACCESS");

        return Jwts.builder()
                .setClaims(claims)
                .setId(jti) // JTI (JWT ID) - unique identifier
                .setSubject(emp.getEmail())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 120 * 60 * 1000)) // 10 minutes
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
    }

    // Generate Refresh Token (30 minutes)
    public String generateRefreshToken(Employee emp, String deviceId, String jti) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("deviceId", deviceId);
        claims.put("type", "REFRESH");

        return Jwts.builder()
                .setClaims(claims)
                .setId(jti) // JTI (JWT ID) - unique identifier
                .setSubject(emp.getEmail())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 180 * 60 * 1000)) // 30 minutes
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
    }

    // Validate token structure (not just expiration)
    public boolean validateTokenStructure(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}