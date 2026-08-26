package com.dmsBackend.response;

import lombok.Data;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;

@Data
public class ReportEntryCaseResponse {
    private Integer documentHeaderId;
    private String caseId;
    private String fileNo;
    private String title;
    private String subject;
    private String firNumber;
    private String policeStation;
    private String evidenceIds;   // comma-joined document_detail ids assigned to this examiner, for display
    private String evidenceTypeName;
    private String divisionName;

    private List<EvidenceRow> myEvidenceList; // ONLY the evidence assigned to the current user
    private String reportStatus; // NOT_STARTED / DRAFT / SUBMITTED

    // Prefill if a draft/submitted report already exists
    private Date examinationStartDate;
    private Date examinationEndDate;
    private Integer examinationMethodId;
    private String observations;
    private String scientificOpinion;
    private String examinationRemarks;
    private Date reportDate;
    private String reportTitle;
    private String reportSummary;
    private String scientificReportPath;
    private List<AttachmentRow> attachments;
    private Timestamp submittedOn;

    @Data
    public static class EvidenceRow {
        private Integer documentDetailId;
        private String docName;
        private String evidenceCategory;
        private String evidenceType;
        private String evidenceDescription;
    }

    @Data
    public static class AttachmentRow {
        private Integer id;
        private String fileName;
        private String filePath;
    }
}
