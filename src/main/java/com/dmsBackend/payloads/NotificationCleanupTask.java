package com.dmsBackend.payloads;

import com.dmsBackend.entity.Notification;
import com.dmsBackend.repository.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;

@Component
@Slf4j
public class NotificationCleanupTask {

    @Autowired
    private NotificationRepository notificationRepository;

    // Method to mark notification as read and immediately remove it
    public void markAsReadAndRemove(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setRead(true);
            notificationRepository.save(notification);
            log.info("Notification {} marked as read", notificationId);
            // Immediately remove the read notification
            notificationRepository.delete(notification);
            log.info("Notification {} removed after being marked as read", notificationId);
        });
    }

    // Optional method to clean up all read notifications
    public void cleanupAllReadNotifications() {
        log.info("Cleaning up all read notifications");
        notificationRepository.deleteByIsReadTrue();
    }
}