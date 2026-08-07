package com.dmsBackend.ArchiveCodes;

import lombok.Data;

@Data
public class ObjectResponse {
    private String statusName;          // e.g., "ok" or "failed"
    private String statusDescription;   // failure reason or description
    private boolean inserted;           // indicates if object archived successfully
}
