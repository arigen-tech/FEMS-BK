package com.dmsBackend.ArchiveCodes;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentArchiveResponse {
    private String statusDescription;
    private String requestId;
    private String statusName;   // success / failed
    private Long statusCode;
}