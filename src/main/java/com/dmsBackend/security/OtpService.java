package com.dmsBackend.security;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    // Stores 2Factor session ID mapped to email
    private final Map<String, String> sessionMap = new ConcurrentHashMap<>();

    public void storeSessionId(String email, String sessionId) {
        sessionMap.put(email, sessionId);
    }

    public String getSessionId(String email) {
        return sessionMap.get(email);
    }

    public void clearSessionId(String email) {
        sessionMap.remove(email);
    }




    // Stores 2Factor session ID mapped to identifier (email/mobile) for forgot password
    private final Map<String, String> forgotPasswordSessionMap = new ConcurrentHashMap<>();

    // Stores user identifier for forgot password flow
    private final Map<String, String> forgotPasswordUserMap = new ConcurrentHashMap<>();

    // Existing login methods



    // Forgot password methods
    public void storeForgotPasswordSession(String identifier, String sessionId, String userEmail) {
        forgotPasswordSessionMap.put(identifier, sessionId);
        forgotPasswordUserMap.put(identifier, userEmail);
    }

    public String getForgotPasswordSessionId(String identifier) {
        return forgotPasswordSessionMap.get(identifier);
    }

    public String getForgotPasswordUserEmail(String identifier) {
        return forgotPasswordUserMap.get(identifier);
    }

    public void clearForgotPasswordSession(String identifier) {
        forgotPasswordSessionMap.remove(identifier);
        forgotPasswordUserMap.remove(identifier);
    }
}
