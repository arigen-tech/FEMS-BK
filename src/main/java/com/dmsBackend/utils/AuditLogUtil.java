package com.dmsBackend.utils;

import com.dmsBackend.entity.Employee;
import com.dmsBackend.response.DocumentDetailsResponse;
import com.dmsBackend.response.DocumentsAuditLogRequest;
import com.dmsBackend.service.DocumentsAuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
@Component
@RequiredArgsConstructor
public class AuditLogUtil {

    private final @Lazy DocumentsAuditLogService auditLogService;


    /**
     * Generic audit logging (used for non-document entities like Branch, Department, etc.)
     */
    public void logAction(Employee employee,
                          String formName,
                          String activity,
                          String status,
                          Integer entityId,
                          String entityName,
                          Integer uniqueId,
                          Map<String, Object> details,
                          HttpServletRequest request) {
        try {
            DocumentsAuditLogRequest logRequest = new DocumentsAuditLogRequest();
            logRequest.setFormName(formName);
            logRequest.setActivity(activity);
            logRequest.setStatus(status);
            logRequest.setLoginAt(LocalDateTime.now());
            logRequest.setUniqueId(uniqueId);
            logRequest.setIpAddress(IpUtils.getClientIp(request));

            // ✅ keep document-related columns null
            logRequest.setDocumentId(null);
            logRequest.setDocumentDetailsId(null);
            logRequest.setDocumentName(null);

            if (employee != null) {
                logRequest.setEmployeeId(employee.getId());
            }

            logRequest.setDetailsJson(details != null ? details : Map.of(
                    "entityId", entityId,
                    "entityName", entityName
            ));

            auditLogService.createLog(logRequest);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Special audit logging for documents (one row per file detail).
     */
    public void logDocumentAction(Employee employee,
                                  String formName,
                                  String activity,
                                  String status,
                                  Integer documentId,
                                  List<DocumentDetailsResponse> documentDetails,
                                  Map<String, Object> detailsJson,
                                  HttpServletRequest request) {
        try {
            if (documentDetails != null && !documentDetails.isEmpty()) {
                for (DocumentDetailsResponse fileDetail : documentDetails) {
                    DocumentsAuditLogRequest logRequest = new DocumentsAuditLogRequest();
                    logRequest.setFormName(formName);
                    logRequest.setActivity(activity);
                    logRequest.setStatus(status);
                    logRequest.setLoginAt(LocalDateTime.now());
                    logRequest.setIpAddress(IpUtils.getClientIp(request));

                    logRequest.setDocumentId(documentId);
                    logRequest.setDocumentDetailsId(fileDetail.getId());
                    logRequest.setDocumentName(fileDetail.getDocName());

                    if (employee != null) {
                        logRequest.setEmployeeId(employee.getId());
                    }

                    // ✅ Use the passed JSON instead of creating a new map
                    logRequest.setDetailsJson(detailsJson);

                    auditLogService.createLog(logRequest);
                }
            } else {
                // fallback → no file details
                DocumentsAuditLogRequest logRequest = new DocumentsAuditLogRequest();
                logRequest.setFormName(formName);
                logRequest.setActivity(activity);
                logRequest.setStatus(status);
                logRequest.setLoginAt(LocalDateTime.now());
                logRequest.setIpAddress(IpUtils.getClientIp(request));
                logRequest.setDocumentId(documentId);
                logRequest.setDocumentName("No File");

                if (employee != null) {
                    logRequest.setEmployeeId(employee.getId());
                }

                logRequest.setDetailsJson(Map.of("info", "No file details"));

                auditLogService.createLog(logRequest);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
