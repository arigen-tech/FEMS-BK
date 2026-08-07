package com.dmsBackend.response;


import com.dmsBackend.entity.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.sql.Timestamp;

@Data
@Builder
public  class NotificationDTO {
    private Long id;
    private String title;
    private String message;
    private String detailedMessage; // This will now contain HTML content
    private NotificationType type;
    private boolean isRead;
    private Timestamp createdOn;
    private Integer referenceId;
    private String referenceType;

    // Add a flag to indicate if the detailed message contains HTML
    private boolean hasHtmlContent = true;
}