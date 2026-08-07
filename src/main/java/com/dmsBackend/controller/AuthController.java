package com.dmsBackend.controller;

import com.dmsBackend.entity.Employee;
import com.dmsBackend.entity.LanguageMaster;
import com.dmsBackend.entity.RoleMaster;
import com.dmsBackend.entity.UserSession;
import com.dmsBackend.repository.LanguageMasterRepository;
import com.dmsBackend.repository.UserSessionRepository;
import com.dmsBackend.response.DocumentsAuditLogRequest;
import com.dmsBackend.response.SimpleLoginRequest;
import com.dmsBackend.security.EmailService;
import com.dmsBackend.security.JwtUtil;
import com.dmsBackend.security.OtpService;
import com.dmsBackend.service.DocumentsAuditLogService;
import com.dmsBackend.service.EmployeeService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import lombok.Data;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;


@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserSessionRepository userSessionRepository;
    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private OtpService otpService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private DocumentsAuditLogService documentsAuditLogService;

    @Autowired
    private LanguageMasterRepository languageMasterRepository;

    @Autowired(required = false)
    private PasswordEncoder passwordEncoder;


    @Value("${verifyOtp}")
    private boolean verifyOtp;

    @Value("verify")
    private String verifyOtpUrl;

    @Value("afterMobile")
    private String afterMobileUrl;

    @Value("beforeMobile")
    private String beforeMobileUrl;

    @GetMapping("/is-otp-enabled")
    public ResponseEntity<Boolean> getVerifyOtpFlag() {
        return ResponseEntity.ok(verifyOtp);
    }


    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid AuthRequest authRequest, HttpServletRequest request) {

        String email = authRequest.getEmail();
        String password = authRequest.getPassword();

        logger.info("========== LOGIN ATTEMPT START ==========");
        logger.info("Email: {}", email);
        logger.info("Password length: {}", password != null ? password.length() : "null");
//        logger.info("LanguageId: {}", authRequest.getLanguageId());

        // Validation
        if (email == null || email.trim().isEmpty()) {
            logger.warn("Email is empty");
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "Email cannot be empty");
        }
        if (password == null || password.trim().isEmpty()) {
            logger.warn("Password is empty");
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "Password cannot be empty");
        }

        try {
            // Step 1: Find employee by email
            logger.info("Step 1: Finding employee by email: {}", email);
            Employee employee = employeeService.findByEmail(email);

            if (employee == null) {
                logger.error("❌ Employee not found in database for email: {}", email);
                saveAuditLog(null, "LoginPage", "User Login", "Failed - User not found", authRequest, request);
                return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid email or password");
            }

            logger.info("✅ Employee found: {}", employee.getName());
            logger.info("   - Email: {}", employee.getEmail());
            logger.info("   - Active: {}", employee.isActive());
            logger.info("   - Role: {}", employee.getRole() != null ? employee.getRole().getRole() : "null");

            // Step 2: Check if employee is active
            if (!employee.isActive()) {
                logger.error("❌ Employee is inactive: {}", email);
                saveAuditLog(employee, "LoginPage", "User Login", "Failed - Account inactive", authRequest, request);
                return buildErrorResponse(HttpStatus.FORBIDDEN, "Your account is inactive. Contact the administrator.");
            }

            // Step 3: Validate role
            String role = employee.getRole() != null ? employee.getRole().getRole() : null;
            if (role == null || role.isEmpty() || "Unknown".equalsIgnoreCase(role)) {
                logger.error("❌ Invalid role for user: {}", email);
                saveAuditLog(employee, "LoginPage", "User Login", "Failed - No role assigned", authRequest, request);
                return buildErrorResponse(HttpStatus.FORBIDDEN, "Role not valid. Contact the administrator.");
            }
            logger.info("✅ Role validation passed: {}", role);

            // Step 4: Authenticate with Spring Security
            logger.info("Step 4: Attempting authentication with AuthenticationManager...");
            Authentication authentication;
            try {
                authentication = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(email, password)
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
                logger.info("✅ Authentication successful!");
            } catch (BadCredentialsException e) {
                logger.error("❌ Authentication failed - Bad credentials");
                logger.error("   Exception: {}", e.getMessage());

                // DEBUGGING: Check password manually if encoder available
                if (passwordEncoder != null && employee.getPassword() != null) {
                    boolean passwordMatches = passwordEncoder.matches(password, employee.getPassword());
                    logger.warn("   Password matches (using encoder): {}", passwordMatches);
                }

                saveAuditLog(employee, "LoginPage", "User Login", "Failed - Bad credentials", authRequest, request);
                return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid email or password");
            } catch (Exception e) {
                logger.error("❌ Unexpected authentication error: {}", e.getMessage(), e);
                saveAuditLog(employee, "LoginPage", "User Login", "Failed - Auth error", authRequest, request);
                return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Authentication error. Please try again.");
            }

            // Step 5: Update language preference
//            if (authRequest.getLanguageId() != null) {
//                employeeService.updateLanguageOnly(employee.getId(), authRequest.getLanguageId());
//            }

            // Step 6: Save audit log and send OTP
            logger.info("Step 6: Saving audit log and sending OTP...");
            saveAuditLog(employee, "LoginPage", "User Login", "Success - OTP sent", authRequest, request);
            if (verifyOtp) {
            logger.info("========== LOGIN ATTEMPT SUCCESS ==========");
            return sendOtpForLogin(employee, email, role);}
            else{
                OtpRequest req=new OtpRequest();
                req.setEmail(authRequest.getEmail());
                req.setDeviceId(authRequest.getDeviceId());
                return verifyOtp(req,request);
            }


        } catch (Exception e) {
            logger.error("========== UNEXPECTED ERROR DURING LOGIN ==========", e);
            logger.error("Exception type: {}", e.getClass().getName());
            logger.error("Exception message: {}", e.getMessage());
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred. Please try again.");
        }


    }

    @PostMapping("/verifyOtp")
    public ResponseEntity<?> verifyOtp(@RequestBody @Valid OtpRequest otpRequest,
                                       HttpServletRequest httpRequest) {
        logger.info("OTP Verification attempt for email: {}", otpRequest.getEmail());

        try {
            String email = otpRequest.getEmail();
            String otp = otpRequest.getOtp();
            String deviceId = otpRequest.getDeviceId();
            String sessionId = otpService.getSessionId(email);
            if (verifyOtp) {
                if (sessionId == null) {
                    logger.warn("OTP session not found or expired for: {}", email);
                    return createErrorResponse("OTP session expired or not found.");
                }

                logger.info("Verifying OTP for session: {}", sessionId);
                String uri = verifyOtpUrl
                        + sessionId + "/" + otp;

                HttpResponse<String> response = Unirest.post(uri)
                        .header("content-type", "application/x-www-form-urlencoded").asString();

                JSONObject json = new JSONObject(response.getBody());
                String status = json.getString("Status");

                if (!status.equalsIgnoreCase("Success")) {
                    logger.warn("OTP verification failed. Status: {}", status);
                    return createErrorResponse("Invalid OTP.");
                }

                logger.info("✅ OTP verified successfully!");
                otpService.clearSessionId(email);
            }
            Employee employee = employeeService.findByEmail(email);
            if (employee == null) {
                logger.error("Employee not found after OTP verification: {}", email);
                return createErrorResponse("Employee not found.");
            }

            RoleMaster currentRole = employee.getRole();

            // Check role max devices
            if (currentRole.getMaxDevices() == null) {
                currentRole.setMaxDevices(1); // Default to 1 if not set
            }

            int maxDevices = currentRole.getMaxDevices();

            // Get active sessions for this employee and role
            List<UserSession> activeSessionsForRole = userSessionRepository
                    .findByEmployeeIdAndRoleAndActiveTrue(employee.getId(), currentRole);

            logger.info("Active sessions for role {}: {} (max: {})",
                    currentRole.getRole(), activeSessionsForRole.size(), maxDevices);

            // If limit exceeded, deactivate oldest session for this role
            if (activeSessionsForRole.size() >= maxDevices) {
                logger.info("Device limit ({}) exceeded for role: {}. Deactivating oldest session.",
                        maxDevices, currentRole.getRole());

                int deactivated = userSessionRepository.deactivateOldestSession(
                        employee.getId(), currentRole.getId());

                logger.info("Deactivated {} oldest session(s)", deactivated);
            }

            // Generate unique IDs for tokens
            String accessJti = UUID.randomUUID().toString();
            String refreshJti = UUID.randomUUID().toString();

            // Generate tokens
            String accessToken = jwtUtil.generateAccessToken(employee, deviceId, accessJti);
            String refreshToken = jwtUtil.generateRefreshToken(employee, deviceId, refreshJti);

            // Create new session
            UserSession newSession = new UserSession();
            newSession.setEmployee(employee);
            newSession.setRole(currentRole);
            newSession.setDeviceId(deviceId);
            newSession.setAccessTokenJti(accessJti);
            newSession.setRefreshTokenJti(refreshJti);
            newSession.setActive(true);
            newSession.setIpAddress(getClientIp(httpRequest));
            newSession.setUserAgent(httpRequest.getHeader("User-Agent"));
            newSession.setAccessTokenExpiry(new Date(System.currentTimeMillis() + 10 * 60 * 1000));
            newSession.setRefreshTokenExpiry(new Date(System.currentTimeMillis() + 30 * 60 * 1000));

            userSessionRepository.save(newSession);

            // Get language
            String languageCode = "en";
            if (employee.getLanguage() != null && employee.getLanguage().getCode() != null) {
                languageCode = employee.getLanguage().getCode();
            }

            logger.info("✅ Login successful for user: {} on device: {} with role: {}",
                    employee.getEmail(), deviceId, currentRole.getRole());

            // Return response with both tokens
            return ResponseEntity.ok(new AuthResponse(
                    accessToken,
                    refreshToken,
                    "OTP verified successfully.",
                    currentRole.getRole(),
                    currentRole.getId(),
                    employee.getName(),
                    employee.getId(),
                    languageCode
            ));

        } catch (Exception e) {
            logger.error("Error during OTP verification: {}", e.getMessage(), e);
            return createErrorResponse("Unable to verify OTP. Please try again.");
        }
    }
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshRequest refreshRequest, HttpServletRequest httpRequest) {
        try {
            String refreshToken = refreshRequest.getRefreshToken();
            String deviceId = refreshRequest.getDeviceId();

            if (!jwtUtil.validateTokenStructure(refreshToken)) {
                return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
            }

            // Extract claims
            String username = jwtUtil.extractUsername(refreshToken);
            String jti = jwtUtil.extractJti(refreshToken);
            String tokenDeviceId = jwtUtil.extractDeviceId(refreshToken);
            String tokenType = jwtUtil.extractClaim(refreshToken, claims -> claims.get("type", String.class));

            if (!"REFRESH".equals(tokenType)) {
                return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid token type");
            }

            if (!deviceId.equals(tokenDeviceId)) {
                return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Device mismatch");
            }

            // Check if refresh token is valid in database
            UserSession session = userSessionRepository.findByRefreshTokenJtiAndActiveTrue(jti)
                    .orElse(null);

            if (session == null) {
                return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Session expired");
            }

            if (jwtUtil.isTokenExpired(refreshToken)) {
                // Refresh token expired, log out the session
                session.setActive(false);
                userSessionRepository.save(session);
                return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Refresh token expired");
            }

            Employee employee = employeeService.findByEmail(username);
            if (employee == null) {
                return buildErrorResponse(HttpStatus.NOT_FOUND, "User not found");
            }

            // Generate new access token
            String newAccessJti = UUID.randomUUID().toString();
            String newAccessToken = jwtUtil.generateAccessToken(employee, deviceId, newAccessJti);

            // Update session with new access token JTI
            session.setAccessTokenJti(newAccessJti);
            session.setAccessTokenExpiry(new Date(System.currentTimeMillis() + 10 * 60 * 1000));
            userSessionRepository.save(session);

            return ResponseEntity.ok(new AuthResponse(
                    newAccessToken,
                    refreshToken, // Return same refresh token (or rotate if you want)
                    "Token refreshed successfully",
                    employee.getRole().getRole(),
                    employee.getRole().getId(),
                    employee.getName(),
                    employee.getId(),
                    employee.getLanguage() != null ? employee.getLanguage().getCode() : "en"
            ));

        } catch (Exception e) {
            logger.error("Error refreshing token: {}", e.getMessage(), e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Error refreshing token");
        }
    }

    // Add this DTO class inside AuthController
    public static class RefreshRequest {
        @NotBlank
        private String refreshToken;

        @NotBlank
        private String deviceId;

        // Getters and setters
        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader,
                                    @RequestParam String deviceId) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid authorization header");
            }

            String token = authHeader.substring(7);
            String jti = jwtUtil.extractJti(token);

            // Find and deactivate the session
            UserSession session = userSessionRepository.findByAccessTokenJtiAndActiveTrue(jti)
                    .orElse(null);

            if (session != null && session.getDeviceId().equals(deviceId)) {
                session.setActive(false);
                userSessionRepository.save(session);
                logger.info("User logged out from device: {}", deviceId);
            }

            return ResponseEntity.ok(new ApiResponse("success", "Logged out successfully", null));

        } catch (Exception e) {
            logger.error("Error during logout: {}", e.getMessage());
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Error during logout");
        }
    }

    @PostMapping("/dms-login-outside")
    public ResponseEntity<?> simpleLogin(@RequestBody @Valid SimpleLoginRequest loginRequest,
                                         HttpServletRequest httpRequest) {

        String identifier = loginRequest.getIdentifier();
        String password = loginRequest.getPassword();
        String deviceId = "asdf!@12";

        logger.info("========== DMS LOGIN ATTEMPT START ==========");
        logger.info("Identifier: {}", identifier);

        if (identifier == null || identifier.trim().isEmpty()) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "Email or mobile cannot be empty");
        }

        if (password == null || password.trim().isEmpty()) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "Password cannot be empty");
        }

        try {

            Optional<Employee> optionalEmployee = employeeService.findByEmailOrMobile(identifier);

            AuthRequest auditRequest = new AuthRequest();
            auditRequest.setEmail(identifier);
            auditRequest.setPassword(password);
            auditRequest.setDeviceId(deviceId);

            if (optionalEmployee.isEmpty()) {
                saveAuditLog(null, "DmsLogin", "User Login", "Failed - User not found", auditRequest, httpRequest);
                return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid credentials");
            }

            Employee employee = optionalEmployee.get();

            if (!employee.isActive()) {
                saveAuditLog(employee, "DmsLogin", "User Login", "Failed - Account inactive", auditRequest, httpRequest);
                return buildErrorResponse(HttpStatus.FORBIDDEN,
                        "Your account is inactive. Contact the administrator.");
            }

            String role = employee.getRole() != null ? employee.getRole().getRole() : null;

            if (role == null || role.isEmpty() || "Unknown".equalsIgnoreCase(role)) {
                saveAuditLog(employee, "DmsLogin", "User Login", "Failed - Invalid role", auditRequest, httpRequest);
                return buildErrorResponse(HttpStatus.FORBIDDEN,
                        "Role not valid. Contact the administrator.");
            }

            if (!passwordEncoder.matches(password, employee.getPassword())) {
                saveAuditLog(employee, "DmsLogin", "User Login", "Failed - Bad credentials", auditRequest, httpRequest);
                return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid credentials");
            }

            // Spring Security authentication
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(employee.getEmail(), password)
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            RoleMaster currentRole = employee.getRole();

            if (currentRole.getMaxDevices() == null) {
                currentRole.setMaxDevices(1);
            }

            int maxDevices = currentRole.getMaxDevices();

            List<UserSession> activeSessionsForRole =
                    userSessionRepository.findByEmployeeIdAndRoleAndActiveTrue(employee.getId(), currentRole);

            logger.info("Active sessions for role {}: {} (max: {})",
                    currentRole.getRole(), activeSessionsForRole.size(), maxDevices);

            if (activeSessionsForRole.size() >= maxDevices) {

                logger.info("Device limit exceeded for role {}, deactivating oldest session",
                        currentRole.getRole());

                userSessionRepository.deactivateOldestSession(employee.getId(), currentRole.getId());
            }

            // Generate token IDs
            String accessJti = UUID.randomUUID().toString();
            String refreshJti = UUID.randomUUID().toString();

            // Generate tokens
            String accessToken = jwtUtil.generateAccessToken(employee, deviceId, accessJti);
            String refreshToken = jwtUtil.generateRefreshToken(employee, deviceId, refreshJti);

            // Save session
            UserSession newSession = new UserSession();
            newSession.setEmployee(employee);
            newSession.setRole(currentRole);
            newSession.setDeviceId(deviceId);
            newSession.setAccessTokenJti(accessJti);
            newSession.setRefreshTokenJti(refreshJti);
            newSession.setActive(true);
            newSession.setIpAddress(getClientIp(httpRequest));
            newSession.setUserAgent(httpRequest.getHeader("User-Agent"));
            newSession.setAccessTokenExpiry(new Date(System.currentTimeMillis() + 10 * 60 * 1000));
            newSession.setRefreshTokenExpiry(new Date(System.currentTimeMillis() + 30 * 60 * 1000));

            userSessionRepository.save(newSession);

            saveAuditLog(employee, "DmsLogin", "User Login", "Success - Login successful", auditRequest, httpRequest);

            String languageCode = "en";
            if (employee.getLanguage() != null && employee.getLanguage().getCode() != null) {
                languageCode = employee.getLanguage().getCode();
            }

            logger.info("✅ DMS login successful for {}", employee.getEmail());

            return ResponseEntity.ok(new AuthResponse(
                    accessToken,
                    refreshToken,
                    "Login successful",
                    currentRole.getRole(),
                    currentRole.getId(),
                    employee.getName(),
                    employee.getId(),
                    languageCode
            ));

        } catch (Exception e) {
            logger.error("Error during DMS login", e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "An unexpected error occurred. Please try again.");
        }
    }
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        logger.info("Forgot password request for identifier: {}", request.getIdentifier());

        try {
            if (request.getIdentifier() == null || request.getIdentifier().trim().isEmpty()) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, "Email is required");
            }

            Optional<Employee> optionalEmployee = employeeService.findByEmailOrMobile(request.getIdentifier());

            if (optionalEmployee.isEmpty()) {
                logger.warn("No user found for identifier: {}", request.getIdentifier());
                return buildErrorResponse(HttpStatus.NOT_FOUND, "You are not registered with this email.");
            }

            Employee employee = optionalEmployee.get();

            if (!employee.isActive()) {
                logger.warn("Inactive user attempted password reset: {}", request.getIdentifier());
                return buildErrorResponse(HttpStatus.FORBIDDEN, "Your account is inactive. Please contact the administrator.");
            }

            boolean isEmail = request.getIdentifier().contains("@");

            if (isEmail) {
                return sendOtpViaEmail(employee, request.getIdentifier());
            }
            // else {
            //     return sendOtpViaSms(employee, request.getIdentifier());
            // }

            return buildErrorResponse(HttpStatus.BAD_REQUEST, "Only email is supported for password reset.");

        } catch (Exception e) {
            logger.error("Error in forgot password: {}", e.getMessage(), e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred. Please try again.");
        }
    }



    private ResponseEntity<?> sendOtpViaSms(Employee employee, String identifier) {
        try {
            String mobile = employee.getMobile();
            logger.info("Sending OTP to mobile: {}", mobile);

            String uri = beforeMobileUrl
                    + mobile + afterMobileUrl;

            HttpResponse<String> response = Unirest.post(uri).asString();
            JSONObject jsonObject = new JSONObject(response.getBody());

            if (!jsonObject.getString("Status").equalsIgnoreCase("Success")) {
                logger.error("Failed to send OTP via SMS");
                return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to send OTP.");
            }

            String sessionId = jsonObject.getString("Details");
            otpService.storeForgotPasswordSession(identifier, sessionId, employee.getEmail());
            logger.info("✅ OTP sent successfully. Session ID: {}", sessionId);

            return ResponseEntity.ok(new AuthResponse(null, "OTP sent to your registered mobile number.", null, null, null, null, null));

        } catch (Exception e) {
            logger.error("Error sending SMS OTP: {}", e.getMessage(), e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to send OTP.");
        }
    }

    private ResponseEntity<?> sendOtpViaEmail(Employee employee, String identifier) {
        try {
            logger.info("Sending OTP via EMAIL to: {}", employee.getEmail());

            // Generate 6 digit OTP
            String otp = String.valueOf(new Random().nextInt(900000) + 100000);

            // Store OTP in memory (use identifier as key)
            otpService.storeForgotPasswordSession(identifier, otp, employee.getEmail());

            // Send OTP via email
            emailService.sendOtp(employee.getEmail(), otp, employee);

            logger.info("✅ Email OTP sent successfully to {}", employee.getEmail());

            return ResponseEntity.ok(
                    new AuthResponse(
                            null,
                            "OTP sent to your registered email.",
                            null, null, null, null, null
                    )
            );

        } catch (Exception e) {
            logger.error("Error sending email OTP: {}", e.getMessage(), e);
            return buildErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to send OTP."
            );
        }
    }

    @PostMapping("/verify-forgot-password-otp")
    public ResponseEntity<?> verifyForgotPasswordOtp(@RequestBody @Valid VerifyResetOtpRequest request) {

        logger.info("Verifying forgot password OTP for identifier: {}", request.getIdentifier());

        try {
            String identifier = request.getIdentifier();
            String otp = request.getOtp();

            String storedOtp = otpService.getForgotPasswordSessionId(identifier);

            if (storedOtp == null) {
                logger.warn("Forgot password OTP session expired for: {}", identifier);
                return createErrorResponse("OTP session expired. Please request again.");
            }

            if (!storedOtp.equals(otp)) {
                logger.warn("Invalid OTP entered for: {}", identifier);
                return createErrorResponse("Invalid OTP.");
            }

            logger.info("✅ Forgot password OTP verified successfully!");

            return ResponseEntity.ok(
                    new AuthResponse(
                            null,
                            "OTP verified successfully. You can now reset your password.",
                            null, null, null, null, null
                    )
            );

        } catch (Exception e) {
            logger.error("Error verifying forgot password OTP: {}", e.getMessage(), e);
            return createErrorResponse("Unable to verify OTP. Please try again.");
        }
    }
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {

        logger.info("Password reset request for identifier: {}", request.getIdentifier());

        try {
            String identifier = request.getIdentifier();
            String otp = request.getOtp();
            String newPassword = request.getNewPassword();
            String confirmPassword = request.getConfirmPassword();

            if (!newPassword.equals(confirmPassword)) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, "Passwords do not match.");
            }

            if (newPassword.length() < 6) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, "Password must be at least 6 characters long.");
            }

            String storedOtp = otpService.getForgotPasswordSessionId(identifier);

            if (storedOtp == null) {
                return createErrorResponse("Session expired. Please request a new OTP.");
            }

            if (!storedOtp.equals(otp)) {
                return createErrorResponse("Invalid OTP.");
            }

            String userEmail = otpService.getForgotPasswordUserEmail(identifier);

            employeeService.resetPassword(userEmail, newPassword);

            // Clear OTP after success
            otpService.clearForgotPasswordSession(identifier);

            logger.info("✅ Password reset successful for: {}", userEmail);

            return ResponseEntity.ok(
                    new AuthResponse(
                            null,
                            "Password reset successfully.",
                            null, null, null, null, null
                    )
            );

        } catch (Exception e) {
            logger.error("Error resetting password: {}", e.getMessage(), e);
            return createErrorResponse("Unable to reset password. Please try again.");
        }
    }

    private void saveAuditLog(Employee employee,
                              String formName,
                              String activity,
                              String status,
                              AuthRequest authRequest,
                              HttpServletRequest request) {
        try {
            String ipAddress = getClientIp(request);

            DocumentsAuditLogRequest logRequest = new DocumentsAuditLogRequest();
            logRequest.setFormName(formName);
            logRequest.setActivity(activity);
            logRequest.setStatus(status);
            logRequest.setIpAddress(ipAddress);
            logRequest.setLoginAt(LocalDateTime.now());

            if (employee == null) {
                logRequest.setDetailsJson(Map.of("email", authRequest.getEmail()));
            } else {
                logRequest.setDetailsJson(Map.of(
                        "email", employee.getEmail(),
                        "role", employee.getRole() != null ? employee.getRole().getRole() : "N/A"
                ));
            }

            documentsAuditLogService.createLog(logRequest);

        } catch (Exception ex) {
            logger.error("Failed to save audit log: {}", ex.getMessage());
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = null;

        String[] headers = {
                "X-Forwarded-For",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_X_FORWARDED",
                "HTTP_X_CLUSTER_CLIENT_IP",
                "HTTP_CLIENT_IP",
                "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED",
                "HTTP_VIA",
                "REMOTE_ADDR"
        };

        for (String header : headers) {
            String value = request.getHeader(header);
            if (value != null && !value.isEmpty() && !"unknown".equalsIgnoreCase(value)) {
                ip = value.split(",")[0].trim();
                break;
            }
        }

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            ip = "127.0.0.1 (localhost)";
        }

        return ip;
    }

    private ResponseEntity<?> sendOtpForLogin(Employee employee, String email, String roles) {
        Long mobile = Long.valueOf(employee.getMobile());

        try {
            logger.info("Sending OTP for login to mobile: {}", mobile);

            String uri = beforeMobileUrl
                    + mobile + afterMobileUrl;

            HttpResponse<String> response = Unirest.post(uri).asString();
            JSONObject jsonObject = new JSONObject(response.getBody());

            if (!jsonObject.getString("Status").equalsIgnoreCase("Success")) {
                logger.error("Failed to send login OTP");
                return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Failed to send OTP.");
            }

            String sessionId = jsonObject.getString("Details");
            otpService.storeSessionId(email, sessionId);
            logger.info("✅ OTP sent successfully for login. Session: {}", sessionId);

            String languageCode = "en";
            if (employee.getLanguage() != null && employee.getLanguage().getCode() != null) {
                languageCode = employee.getLanguage().getCode();
            }

            return ResponseEntity.ok(new AuthResponse(
                    null,
                    "OTP sent to registered mobile number.",
                    roles,
                    employee.getRole().getId(),
                    employee.getName(),
                    employee.getId(),
                    languageCode
            ));
        } catch (Exception e) {
            logger.error("Error sending OTP for login: {}", e.getMessage(), e);
            return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Unable to send OTP.");
        }
    }

    private ResponseEntity<?> buildErrorResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(message));
    }

    private ResponseEntity<ApiResponse> createErrorResponse(String message) {
        return ResponseEntity.badRequest().body(new ApiResponse("error", message, null));
    }

    // ============== DTOs ==============

    public static class ForgotPasswordRequest {
        @jakarta.validation.constraints.NotBlank
        private String identifier;

        public String getIdentifier() { return identifier; }
        public void setIdentifier(String identifier) { this.identifier = identifier; }
    }

    public static class VerifyResetOtpRequest {
        @jakarta.validation.constraints.NotBlank
        private String identifier;

        @jakarta.validation.constraints.NotBlank
        private String otp;

        public String getIdentifier() { return identifier; }
        public void setIdentifier(String identifier) { this.identifier = identifier; }
        public String getOtp() { return otp; }
        public void setOtp(String otp) { this.otp = otp; }
    }

    public static class ResetPasswordRequest {
        @jakarta.validation.constraints.NotBlank
        private String identifier;

        @jakarta.validation.constraints.NotBlank
        private String otp;

        @jakarta.validation.constraints.NotBlank
        private String newPassword;

        @jakarta.validation.constraints.NotBlank
        private String confirmPassword;

        public String getIdentifier() { return identifier; }
        public void setIdentifier(String identifier) { this.identifier = identifier; }
        public String getOtp() { return otp; }
        public void setOtp(String otp) { this.otp = otp; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
        public String getConfirmPassword() { return confirmPassword; }
        public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
    }

    @Data
    public static class AuthRequest {
        @jakarta.validation.constraints.Email
        @jakarta.validation.constraints.NotBlank
        private String email;

        @jakarta.validation.constraints.NotBlank
        private String password;
//        private Long languageId;
        private String deviceId;
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
//        public Long getLanguageId() { return languageId; }
        public String getDeviceId() {
            return deviceId;
        }
//        public void setLanguageId(Long languageId) { this.languageId = languageId; }
    }

    public static class OtpRequest {
        @jakarta.validation.constraints.Email
        @jakarta.validation.constraints.NotBlank
        private String email;

        @jakarta.validation.constraints.NotBlank
        private String otp;

        public String getDeviceId() {
            return deviceId;
        }

        public void setDeviceId(String deviceId) {
            this.deviceId = deviceId;
        }

        private String deviceId;


        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getOtp() { return otp; }
        public void setOtp(String otp) { this.otp = otp; }
    }

    @Data
    public static class AuthResponse {

        private String token;          // access token
        private String refreshToken;   // refresh token
        private String message;
        private String roles;
        private Integer currRoleId;
        private String name;
        private Integer id;
        private String languageCode;

        // ✅ LOGIN SUCCESS (ACCESS + REFRESH)
        public AuthResponse(
                String token,
                String refreshToken,
                String message,
                String roles,
                Integer currRoleId,
                String name,
                Integer id,
                String languageCode
        ) {
            this.token = token;
            this.refreshToken = refreshToken;
            this.message = message;
            this.roles = roles;
            this.currRoleId = currRoleId;
            this.name = name;
            this.id = id;
            this.languageCode = languageCode;
        }

        // ✅ SIMPLE RESPONSE (OTP SENT / RESET PASSWORD ETC)
        public AuthResponse(
                String token,
                String message,
                String roles,
                Integer currRoleId,
                String name,
                Integer id,
                String languageCode
        ) {
            this.token = token;
            this.message = message;
            this.roles = roles;
            this.currRoleId = currRoleId;
            this.name = name;
            this.id = id;
            this.languageCode = languageCode;
        }
    }

    public static class ErrorResponse {
        private String message;

        public ErrorResponse(String message) {
            this.message = message;
        }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public static class ApiResponse {
        private String status;
        private String message;
        private Object data;

        public ApiResponse(String status, String message, Object data) {
            this.status = status;
            this.message = message;
            this.data = data;
        }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Object getData() { return data; }
        public void setData(Object data) { this.data = data; }
    }
}
