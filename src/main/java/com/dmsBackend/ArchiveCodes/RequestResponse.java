package com.dmsBackend.ArchiveCodes;

import lombok.Data;

@Data
public class RequestResponse {
    private String statusName;          // e.g., "ok" or "failed"
    private String statusDescription;   // failure reason or description
    private String requestId;           // optional: if API sends back requestId
    private String stateName;
}
