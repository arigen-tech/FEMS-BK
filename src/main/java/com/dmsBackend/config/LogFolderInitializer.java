package com.dmsBackend.config;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class LogFolderInitializer {

    // Base log folder
    private static final String LOG_PATH = "./logs"; // Relative path

    @PostConstruct
    public void createLogFolder() {
        File logDir = new File(LOG_PATH);
        if (!logDir.exists()) {
            boolean created = logDir.mkdirs();
            if (created) {
                System.out.println("✅ Log directory created at: " + logDir.getAbsolutePath());
            } else {
                System.err.println("❌ Failed to create log directory: " + logDir.getAbsolutePath());
            }
        }
    }
}
