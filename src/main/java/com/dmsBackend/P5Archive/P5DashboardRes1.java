package com.dmsBackend.P5Archive;

import com.dmsBackend.ArchiveCodes.ArchiveJob;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class P5DashboardRes1 {
    private Long id;
    private String policyType;
    private LocalDateTime archivalDateTime;
    private LocalDateTime archivedDate;
    private String archiveName;
    private String branchName;
    private Integer branchId;
    private String departmentName;
    private Integer departmentId;
    private String description;
    private String archiveStatus;
    private ArchiveJob.Status status;
    private Integer totalFiles;
    private Long totalDocuments;
}

