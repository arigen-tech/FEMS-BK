package com.dmsBackend.response;

import jakarta.persistence.Column;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
public class DocumentDetailsResponse {

    private Integer id;

    private String docName;

    private String path;

    private String version;

    private String year;

    private Timestamp createdOn;

    private Timestamp updatedOn;

    private String status;

    private Timestamp approvedOn;

    private String approvedBy;

    private String updetedBy;

    private String rejectionReason;

    private String createdBy;

}
