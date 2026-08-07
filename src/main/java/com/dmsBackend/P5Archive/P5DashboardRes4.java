package com.dmsBackend.P5Archive;

import lombok.Data;

@Data
public class P5DashboardRes4 {
    private Long id;             // DocumentDetails ID
    private String fileName;     // docName
    private String mimeType;     // mimeType
    private Integer pageCounts;  // pageCounts
    private String fileSize;     // human-readable size
    private String status;       // DocApprovalStatus
}
