package com.dmsBackend.response;



import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ExportResponse {
    private String exportId;
    private String fileName;
    private String fileType; // "csv" or "zip"
    private long fileSize;
    private LocalDateTime exportTime;
    private String status; // "IN_PROGRESS", "COMPLETED", "FAILED"
    private String message;
    private int tablesExported;
    private int filesExported;
}
