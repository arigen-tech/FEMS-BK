package com.dmsBackend.controller;

import com.dmsBackend.response.NotificationDTO;
import com.dmsBackend.response.ApiResponse;
import com.dmsBackend.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationDTO>>> getUserNotifications(
            @RequestParam Integer employeeId) {

        log.info("Fetching notifications for EmployeeId={}", employeeId);

        try {
            List<NotificationDTO> notifications =
                    notificationService.getUserNotifications(employeeId);

            ApiResponse<List<NotificationDTO>> response = new ApiResponse<>();
            response.setResponse(notifications);
            response.setStatus(HttpStatus.OK.value());
            response.setMessage("Notifications retrieved successfully");

            log.info("Fetched {} notifications for EmployeeId={}",
                    notifications.size(), employeeId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to fetch notifications for EmployeeId={}: {}",
                    employeeId, e.getMessage(), e);

            ApiResponse<List<NotificationDTO>> errorResponse = new ApiResponse<>();
            errorResponse.setResponse(null);
            errorResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            errorResponse.setMessage("Failed to retrieve notifications");

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(
            @RequestParam Integer employeeId,
            @RequestHeader("Role") String role) {

        log.info("Fetching unread notification count for EmployeeId={}, Role={}",
                employeeId, role);

        try {
            Map<String, Long> counts =
                    notificationService.getUnreadCount(employeeId, role);

            ApiResponse<Map<String, Long>> response = new ApiResponse<>();
            response.setResponse(counts);
            response.setStatus(HttpStatus.OK.value());
            response.setMessage("Unread count retrieved successfully");

            log.info("Unread count fetched for EmployeeId={}: {}",
                    employeeId, counts);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to fetch unread count for EmployeeId={}, Role={}: {}",
                    employeeId, role, e.getMessage(), e);

            ApiResponse<Map<String, Long>> errorResponse = new ApiResponse<>();
            errorResponse.setResponse(Collections.emptyMap());
            errorResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            errorResponse.setMessage("Failed to retrieve unread count: " + e.getMessage());

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long id) {

        log.info("Marking notification as read. NotificationId={}", id);

        try {
            notificationService.markAsRead(id);

            ApiResponse<Void> response = new ApiResponse<>();
            response.setResponse(null);
            response.setStatus(HttpStatus.OK.value());
            response.setMessage("Notification marked as read successfully");

            log.info("Notification marked as read successfully. NotificationId={}", id);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to mark notification as read. NotificationId={}: {}",
                    id, e.getMessage(), e);

            ApiResponse<Void> errorResponse = new ApiResponse<>();
            errorResponse.setResponse(null);
            errorResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            errorResponse.setMessage("Failed to mark notification as read");

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }
}
