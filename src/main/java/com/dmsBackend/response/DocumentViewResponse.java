package com.dmsBackend.response;

import com.dmsBackend.entity.DocApprovalStatus;
import lombok.Data;

import java.sql.Timestamp;
import java.util.List;

@Data
public class DocumentViewResponse {

    // ───── Document Header Info ─────
    private Integer documentId;
    private String title;
    private String fileNo;
    private String subject;
    private String categoryName;
    private String departmentName;
    private String branchName;
    private String employeeName;

    // ───── Audit Info ─────
    private Timestamp createdOn;
    private String createdBy;
    private Timestamp updatedOn;
    private String updatedBy;

    // ───── Files Attached ─────
    private List<FileInfo> files;

    // ───── Metadata ─────
    private List<MetadataInfo> metadata;


    // ─────────────────────────────────────
    // Inner Classes (kept simple for frontend)
    // ─────────────────────────────────────

    @Data
    public static class FileInfo {

        private Integer fileId;
        private String fileName;
        private String version;
        private String year;
        private String mimeType;
        private String fileType;
        private String fileSizeHuman;
        private Integer pageCounts;
        private DocApprovalStatus status;
        private Timestamp approvedDate;

    }

    @Data
    public static class MetadataInfo {

        private String key;
        private String value;
        private Timestamp createdOn;
    }
}
