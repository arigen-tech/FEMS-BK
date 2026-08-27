package com.dmsBackend.response;

import lombok.Getter;
import lombok.Setter;
import java.sql.Date;

@Getter
@Setter
public class DispatchDetailResponse {
    private Integer reportEntryId;
    private String caseNumber;
    private String firNumber;

    private String forwardingAuthorityName;
    private String forwardingDesignation;
    private String forwardingOrganisation;
    private String forwardingLetterNumber;

    private Date dispatchDate;
    private String dispatchReferenceNo;
    private String recipient;
    private String dispatchMode;
    private String dispatchDocumentPath;
    private String dispatchRemarks;
    private Boolean notifyEmail;
    private Boolean notifySms;
    private String dispatchStatus;
}