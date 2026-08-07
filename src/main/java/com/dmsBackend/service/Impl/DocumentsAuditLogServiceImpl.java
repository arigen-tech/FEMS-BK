package com.dmsBackend.service.Impl;

import com.dmsBackend.entity.DocumentsAuditLog;
import com.dmsBackend.entity.Employee;
import com.dmsBackend.repository.DocumentsAuditLogRepository;
import com.dmsBackend.repository.EmployeeRepository;
import com.dmsBackend.response.ApiResponse;
import com.dmsBackend.response.DocumentsAuditLogRequest;
import com.dmsBackend.response.FileCompareResponse;
import com.dmsBackend.service.DocumentsAuditLogService;
import com.dmsBackend.service.EmployeeService;
import com.dmsBackend.utils.CurrentUser;
import com.dmsBackend.utils.ResponseUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class DocumentsAuditLogServiceImpl implements DocumentsAuditLogService {
    @Autowired
    DocumentsAuditLogRepository documentsAuditLogRpository;
    @Autowired
    @Lazy
    CurrentUser currentUser;


//    @Autowired
//    EmployeeService employeeService;

    @Override
    public List<DocumentsAuditLog> findAllDocumentsAuditLog() {
        return documentsAuditLogRpository.findAll();
    }





//    public Employee getCurrentEmployeeOrThrow() {
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//
//        String username = (authentication != null && authentication.isAuthenticated())
//                ? authentication.getName()
//                : null;
//
//        if (username == null) {
//            throw new RuntimeException("Current user not found");
//        }
//
//        Employee currentEmployee = employeeService.findByEmail(username);
//        if (currentEmployee == null) {
//            throw new RuntimeException("Current user not found");
//        }
//
//        return currentEmployee;
//    }


    @Override
    public ApiResponse<DocumentsAuditLog> createLog(DocumentsAuditLogRequest request) {
        try {
            Employee employee = currentUser.getCurrentEmployeeOrThrow();

            DocumentsAuditLog documentsAuditLog = new DocumentsAuditLog();
            documentsAuditLog.setEmployeeId(employee.getId());
            documentsAuditLog.setEmployeeName(employee.getName());
            documentsAuditLog.setFormName(request.getFormName());
            documentsAuditLog.setActivity(request.getActivity());
            documentsAuditLog.setDocumentId(request.getDocumentId());
            documentsAuditLog.setDocumentName(request.getDocumentName());
            documentsAuditLog.setDocumentDetailsId(request.getDocumentDetailsId());
            documentsAuditLog.setBranchId(employee.getBranch() != null ? employee.getBranch().getId() : null);
            documentsAuditLog.setDepartmentId(employee.getDepartment() != null ? employee.getDepartment().getId() : null);
            documentsAuditLog.setStatus(request.getStatus());
            documentsAuditLog.setLoginAt(request.getLoginAt());
            documentsAuditLog.setCreatedAt(LocalDateTime.now());
            documentsAuditLog.setIpAddress(request.getIpAddress());
            documentsAuditLog.setUniqueId(request.getUniqueId());

            // ✅ Directly assign Map
            if (request.getDetailsJson() != null) {
                documentsAuditLog.setDetailsJson(request.getDetailsJson());
            }

            return ResponseUtils.createSuccessResponse(
                    documentsAuditLogRpository.save(documentsAuditLog),
                    new TypeReference<>() {}
            );
        } catch (Exception e) {
            return ResponseUtils.createFailureResponse(
                    null, new TypeReference<>() {},
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value()
            );
        }
    }

    @Override
    public ApiResponse<List<DocumentsAuditLog>> getLogsBetweenDates(LocalDateTime start, LocalDateTime end) {
        List<DocumentsAuditLog> logs = documentsAuditLogRpository.findByCreatedAtBetween(start, end);

        if (logs.isEmpty()) {
            return ResponseUtils.createNotFoundResponse(
                    "No logs found between " + start.toLocalDate() + " and " + end.toLocalDate(),
                    HttpStatus.NOT_FOUND.value()
            );
        }
        return ResponseUtils.createSuccessResponse(
                logs,
                new TypeReference<List<DocumentsAuditLog>>() {}
        );
    }

}
