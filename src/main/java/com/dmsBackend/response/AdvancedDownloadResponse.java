package com.dmsBackend.response;

import lombok.Data;

@Data
public class AdvancedDownloadResponse {
    private int totalFiles;
    private long totalFileSizeBytes;
    private String totalFileSizeHuman;
    private String message;
}
