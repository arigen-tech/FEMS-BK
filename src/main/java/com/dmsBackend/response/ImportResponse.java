package com.dmsBackend.response;

import com.dmsBackend.service.ImportService.ImportMetadata;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class ImportResponse {
    private String importId;
    private boolean success;
    private String message;
    private String error;
    private LocalDateTime timestamp;

    // Import results
    private boolean databaseImported;
    private boolean filesImported;
    private boolean pathsUpdated;

    // Database import details
    private Integer databaseTables;
    private Integer databaseRecords;
    private List<String> selectedTables;

    // Files import details
    private Integer filesImportedCount;
    private Integer filesSkipped;
    private Integer filesReplaced;
    private List<String> selectedFiles;
    // Additional information
    private Map<String, String> pathMappings;
    private ImportMetadata metadata;
    private Map<String, Object> details;
}