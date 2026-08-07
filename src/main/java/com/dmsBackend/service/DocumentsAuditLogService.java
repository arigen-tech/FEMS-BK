package com.dmsBackend.service;

import com.dmsBackend.entity.DocumentsAuditLog;
import com.dmsBackend.response.ApiResponse;
import com.dmsBackend.response.DocumentsAuditLogRequest;

import java.time.LocalDateTime;
import java.util.List;

public interface DocumentsAuditLogService {
    List<DocumentsAuditLog> findAllDocumentsAuditLog();

    ApiResponse<DocumentsAuditLog> createLog(DocumentsAuditLogRequest request);

    ApiResponse<List<DocumentsAuditLog>> getLogsBetweenDates(LocalDateTime start, LocalDateTime end);

}
