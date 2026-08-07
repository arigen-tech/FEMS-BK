package com.dmsBackend.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class DocumentShareRequest {
    private Integer documentHeaderId; // DocumentHeader ID
    private List<Integer> documentDetailIds; // List of DocumentDetails IDs to share
    private List<Integer> recipientIds; // List of employee IDs to share with
    private LocalDateTime endTime; // Optional expiration time
}