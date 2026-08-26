package com.dmsBackend.response;


import lombok.Getter;
import lombok.Setter;
import java.sql.Timestamp;

@Getter
@Setter
public class ReferralListItem {
    private Integer documentDetailId;
    private Integer reportEntryId;
    private String caseNo;
    private String evidenceId;
    private String evidenceType;
    private String division;
    private String fromLaboratoryName;
    private String toLaboratoryName;
    private Timestamp referredOn;
    private String referralStatus;
    private String referralReason;
}
