package com.dmsBackend.response;

import lombok.Getter;
import lombok.Setter;
import java.sql.Timestamp;

@Getter
@Setter
public class ReportReviewListItem {
    private Integer reportEntryId;
    private Integer documentHeaderId;
    private String caseId;
    private String fileNo;
    private String firNumber;
    private String policeStation;
    private String caseTitle;
    private String caseType;
    private String scientificOfficerName;
    private Integer scientificOfficerId;
    private String reportTitle;
    private Timestamp submittedDate;
    private Timestamp reviewedOn;
    private String reviewStatus;
    private Integer evidenceCount;
}