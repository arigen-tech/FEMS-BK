package com.dmsBackend.security;

import com.dmsBackend.entity.UserSession;
import com.dmsBackend.repository.UserSessionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String requestPath = request.getRequestURI();

        // ✅ 1. VERY IMPORTANT – Allow CORS preflight
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        // ✅ 2. Skip JWT for public endpoints
        if (isPublicEndpoint(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendErrorResponse(response, "Authorization required");
            return;
        }

        String token = authHeader.substring(7);

        try {
            // ✅ 3. Validate token structure
            if (!jwtUtil.validateTokenStructure(token)) {
                sendErrorResponse(response, "Invalid token format");
                return;
            }

            // ✅ 4. Check expiration
            if (jwtUtil.isTokenExpired(token)) {
                sendErrorResponse(response, "Token expired");
                return;
            }

            // ✅ 5. Extract claims
            String username = jwtUtil.extractUsername(token);
            String jti = jwtUtil.extractJti(token);
            String tokenDeviceId = jwtUtil.extractDeviceId(token);
            String tokenType = jwtUtil.extractClaim(token, claims -> claims.get("type", String.class));

            if (!"ACCESS".equals(tokenType)) {
                sendErrorResponse(response, "Invalid token type");
                return;
            }

            // ✅ 6. Validate deviceId from request
            String requestDeviceId = request.getHeader("X-Device-Id");

            if (requestDeviceId == null || requestDeviceId.trim().isEmpty()) {
                requestDeviceId = request.getParameter("deviceId");
            }

            if (requestDeviceId == null || requestDeviceId.trim().isEmpty()) {
                sendErrorResponse(response, "Device identification required");
                return;
            }

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                UserSession session = userSessionRepository
                        .findByAccessTokenJtiAndActiveTrue(jti)
                        .orElse(null);

                if (session == null) {
                    sendErrorResponse(response, "Session expired or invalid");
                    return;
                }

                // ✅ Device validation
                if (!session.getDeviceId().equals(tokenDeviceId)) {
                    sendErrorResponse(response, "Token device mismatch");
                    return;
                }

                if (!session.getDeviceId().equals(requestDeviceId)) {
                    sendErrorResponse(response, "Request device mismatch");
                    return;
                }

                // ✅ Authenticate user
                UserDetails userDetails =
                        userDetailsService.loadUserByUsername(username);

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                SecurityContextHolder.getContext().setAuthentication(authToken);
            }

        } catch (Exception e) {
            log.error("JWT validation error: {}", e.getMessage());
            sendErrorResponse(response, "Authentication failed");
            return;
        }

        filterChain.doFilter(request, response);
    }

    // ✅ Corrected public endpoint method
    private boolean isPublicEndpoint(HttpServletRequest request) {
        String path = request.getRequestURI();

        return path.equals("/") ||
                path.startsWith("/auth") ||
                path.startsWith("/swagger") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/webjars") ||
                path.equals("/error");
    }

    private void sendErrorResponse(HttpServletResponse response, String message)
            throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"message\": \"" + message + "\"}");
    }

    private String extractDeviceFingerprint(HttpServletRequest request) {
        StringBuilder fingerprint = new StringBuilder();

        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null) {
            fingerprint.append(userAgent.hashCode());
        }

        String acceptLanguage = request.getHeader("Accept-Language");
        if (acceptLanguage != null) {
            fingerprint.append("|").append(acceptLanguage.hashCode());
        }

        String screenRes = request.getHeader("X-Screen-Resolution");
        if (screenRes != null) {
            fingerprint.append("|").append(screenRes.hashCode());
        }

        String timezone = request.getHeader("X-Timezone");
        if (timezone != null) {
            fingerprint.append("|").append(timezone.hashCode());
        }

        return DigestUtils.md5DigestAsHex(fingerprint.toString().getBytes());
    }
    @Override protected boolean shouldNotFilter(HttpServletRequest request) { String path = request.getServletPath(); return path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs") || path.startsWith("/swagger-resources") || path.startsWith("/auth"); }
}
