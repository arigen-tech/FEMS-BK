package com.dmsBackend.service;


import com.dmsBackend.entity.*;
import com.dmsBackend.response.NotificationDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service

public interface NotificationService {

    public void createDocumentNotification(DocumentDetails detail);
    public void createEmployeeUpdateNotification(Employee employee, String updateType, Map<String, Boolean> changedFields);
    public void markAsRead(Long notificationId);
    public Map<String, Long> getUnreadCount(Integer employeeId, String role);
    public List<NotificationDTO> getUserNotifications(Integer employeeId);
   public void createRoleAssignmentNotification(Employee employee, RoleMaster newRole, Employee assignedBy);
    public void createCustomNotification(Employee employee, String title, String message, String detailedMessage, NotificationType type, Integer referenceId, String referenceType);
    public void createNewEmployeeNotification(Employee newEmployee);
    public void createNewDocumentSavedNotification(DocumentHeader document);

    void createDocumentShareNotification(DocumentShare share);

    void createDocumentShareRevokeNotification(DocumentShare share, String reason);
}
