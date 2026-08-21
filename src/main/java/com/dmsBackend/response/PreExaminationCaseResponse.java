package com.dmsBackend.response;

import lombok.Getter;
import lombok.Setter;
import java.sql.Timestamp;
import java.util.List;

@Getter
@Setter
public class PreExaminationCaseResponse {
    // Case Details (Read Only)
    private Integer documentHeaderId;
    private String fileNo;
    private String title;
    private String firNumber;
    private String policeStation;
    private Timestamp dateOfIncident;
    private String incidentLocation;
    private String priorityName;
    private String caseStatus;

    // Forwarding Authority (Read Only)
    private ForwardingAuthority forwardingAuthority;

    // Evidence List
    private List<EvidenceRow> evidenceList;

    // Pre-Examination Data (Editable)
    private String preExamStatus;
    private Integer purposeId;
    private Integer natureOfExaminationId;
    private Integer noOfParcels;
    private Integer noOfExhibits;
    private String natureOfCase;
    private Integer crimeTypeId;
    private Integer examPriorityId;
    private Integer sealStatusId;
    private String sealVerificationRemarks;
    private Integer parcelConditionId;
    private String parcelConditionOther;

    @Getter
    @Setter
    public static class ForwardingAuthority {
        private String authorityName;
        private String designation;
        private String organisation;
        private String forwardingLetterNumber;
    }

    @Getter
    @Setter
    public static class EvidenceRow {
        private Integer documentDetailId;
        private String docName;
        private String evidenceCategory;
        private String evidenceTypeName;
        private Integer assignedDivisionId;
        private Integer assignedEmployeeId;
        private String assignmentRemark;
    }
}