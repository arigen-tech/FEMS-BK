package com.dmsBackend.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class BulkShareRequest {
    private List<Integer> documentHeaderIds; // List of DocumentHeader IDs
    private List<Integer> recipientIds; // List of employee IDs to share with
    private LocalDateTime endTime; // Optional expiration time
}