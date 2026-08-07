package com.dmsBackend.response;

import lombok.Data;

@Data
public class ScanRequest {
    private int totalPages;
    private String scanType;

}
