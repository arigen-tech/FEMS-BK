package com.dmsBackend.entity;


import lombok.Getter;
import lombok.Setter;
import java.sql.Timestamp;

@Getter
@Setter
public class DispatchListItem {
    private Integer reportEntryId;
    private String caseNumber;
    private String firNumber;
    private String reportNumber;
    private String divisionName;
    private Timestamp approvedDate;
    private String dispatchStatus;
}