package com.dmsBackend.response;

import lombok.Data;

@Data
public class ShareRevokeRequest {
    private Integer shareId; // Changed from Long to Integer
    private String reason;
}