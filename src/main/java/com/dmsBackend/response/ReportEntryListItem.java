package com.dmsBackend.response;


import lombok.Data;

@Data
public class ReportEntryListItem {
    private Integer documentHeaderId;
    private String fileNo;
    private String title;
    private String CaseType;
    private Integer attachedFileCount; // count of evidence assigned to THIS user in this case
    private String status; // Pending / Draft / Submitted
}
