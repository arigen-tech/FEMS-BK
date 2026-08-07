package com.dmsBackend.repository;

import com.dmsBackend.entity.DocumentsAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository

public interface DocumentsAuditLogRepository extends JpaRepository<DocumentsAuditLog,Long> {


    List<DocumentsAuditLog> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
