package com.dmsBackend.response;



import lombok.Data;

@Data
public class ExportRequest {
    private boolean exportDatabase;
    private boolean exportFiles;
    private boolean includeMetadata;
    private String customFileName;
}
