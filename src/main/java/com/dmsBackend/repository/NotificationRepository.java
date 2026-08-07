package com.dmsBackend.repository;


import com.dmsBackend.entity.Notification;
import com.dmsBackend.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
@EnableJpaRepositories
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByEmployeeIdOrderByCreatedOnDesc(Integer employeeId);
    long countByEmployeeIdAndIsReadFalse(Integer employeeId);



    void deleteByIsReadTrue();

    long countByEmployee_IdAndTypeInAndIsReadFalse(Integer employeeId, List<NotificationType> list);

    long countByEmployee_IdAndTypeAndIsReadFalse(Integer employeeId, NotificationType notificationType);

    long countByEmployee_IdAndIsReadFalse(Integer employeeId);
}
