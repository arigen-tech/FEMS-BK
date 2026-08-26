package com.dmsBackend.response;

import lombok.Getter;
import lombok.Setter;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ReportReviewDetailResponse {
    private Integer reportEntryId;
    private Integer documentHeaderId;
    private String caseId;
    private String fileNo;
    private String firNumber;
    private String policeStation;
    private String caseTitle;
    private String subject;
    private String caseType;
    private String scientificOfficerName;
    private Integer scientificOfficerId;
    private String reportTitle;
    private String reportSummary;
    private String observations;
    private String scientificOpinion;
    private String examinationRemarks;
    private Date examinationStartDate;
    private Date examinationEndDate;
    private String examinationMethodName;
    private String status;
    private String reviewStatus;
    private Timestamp submittedOn;
    private String scientificReportPath;
    private String finalReportPath;
    private String referralStatus;
    private String referredToLab;
    private Timestamp referredOn;
    private String referralReason;
    private List<AttachmentRow> attachments = new ArrayList<>();

    @Getter
    @Setter
    public static class AttachmentRow {
        private Integer id;
        private String fileName;
        private String filePath;
    }
}