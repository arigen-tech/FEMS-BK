package com.dmsBackend.response;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EvidenceReviewListItem {
    private Integer documentDetailId;
    private String evidenceId;          // e.g. "EVD-<id>"
    private String evidenceCategory;    // EvidenceTypeMaster.category
    private String evidenceType;        // EvidenceTypeMaster.name
    private String division;            // assigned department name
    private String scientificOfficerName;
    private Integer scientificOfficerId;
    private String reportStatus;        // linked ReportEntry.reviewStatus, else document status
    private String referralStatus;      // linked ReportEntry.referralStatus
    private Integer reportEntryId;      // used by frontend "View" button to open FinalReviewComponent
}
