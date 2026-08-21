package com.dmsBackend.response;

import lombok.Data;
import java.util.List;

@Data
public class PreExaminationRequest {
    private Integer documentHeaderId;
    private Integer purposeId;
    private Integer natureOfExaminationId;
    private Integer noOfParcels;
    private Integer noOfExhibits;
    private String natureOfCase;
    private Integer crimeTypeId;
    private Integer priorityId;
    private Integer sealStatusId;
    private String sealVerificationRemarks;
    private Integer parcelConditionId;
    private String parcelConditionOther;
    private List<EvidenceAssignment> assignments;

    @Data
    public static class EvidenceAssignment {
        private Integer documentDetailId;
        private Integer divisionId;
        private Integer employeeId;
        private String remark;
    }
}
