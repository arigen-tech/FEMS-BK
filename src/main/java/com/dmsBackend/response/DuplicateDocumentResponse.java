package com.dmsBackend.response;

import lombok.Data;

import java.sql.Timestamp;
import java.util.List;

@Data
public class DuplicateDocumentResponse {
    private Integer originalDocumentId;
    private String originalFileName;
    private String originalFilePath;
    private List<DuplicateFileInfo> duplicateFiles;

    @Data
    public static class DuplicateFileInfo {
        private Integer duplicateId;
        private String duplicateFileName;
        private String duplicateFilePath;
        private String version;
        private Timestamp createdOn;
    }
}