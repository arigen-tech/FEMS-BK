package com.dmsBackend.ArchiveCodes;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ArchiveJobDTO {
    private Long id;
    private String policyType;
    private LocalDateTime fromDate;
    private LocalDateTime toDate;
    private LocalDateTime archiveDateTime;
    private LocalDateTime archivedDateTime;
    private String archiveName;
    private String branchName;
    private Integer branchId;
    private String departmentName;
    private Integer departmentId;
    private String description;
    private ArchiveJob.Status status;
    private Integer totalFiles;
    private Integer archivedFiles;
    private Integer failedFiles;
    private Integer archivedDocuments;
    private Integer failedDocuments;
    private Integer totalDocuments;
    private Integer totalVersion;
}
