package com.dmsBackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "dms.export")
public class ExportConfiguration {
    private String tempDirectory = "C:/temp/dms_exports";
    private int cleanupDays = 7;
    private String maxFileSize = "100GB";

    // Storage paths from application.properties
    private String documentStoragePath;
    private String waitingRoomStoragePath;
    private String profileStoragePath;
    private String documentArchivePath;

    // Helper method to get max file size in bytes
    public long getMaxFileSizeInBytes() {
        if (maxFileSize == null || maxFileSize.isEmpty()) {
            return 100L * 1024 * 1024 * 1024; // Default 100GB in bytes
        }

        try {
            String size = maxFileSize.toUpperCase();
            if (size.endsWith("GB")) {
                long gb = Long.parseLong(size.replace("GB", "").trim());
                return gb * 1024L * 1024L * 1024L;
            } else if (size.endsWith("MB")) {
                long mb = Long.parseLong(size.replace("MB", "").trim());
                return mb * 1024L * 1024L;
            } else if (size.endsWith("KB")) {
                long kb = Long.parseLong(size.replace("KB", "").trim());
                return kb * 1024L;
            } else if (size.endsWith("B")) {
                return Long.parseLong(size.replace("B", "").trim());
            } else {
                // Default to 100GB if format is unrecognized
                return 100L * 1024 * 1024 * 1024;
            }
        } catch (NumberFormatException e) {
            // Default to 100GB if parsing fails
            return 100L * 1024 * 1024 * 1024;
        }
    }
}