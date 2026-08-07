package com.dmsBackend.service;

import com.dmsBackend.entity.ActionTypeForReport;
import com.dmsBackend.entity.DocumentDetails;
import com.dmsBackend.entity.DocumentHeader;
import com.dmsBackend.entity.Employee;
import com.dmsBackend.response.DocumentActivityReportRequest;
import com.dmsBackend.response.DocumentActivityReportResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;

public interface DocumentActivityReportService {

    void logAction(
            DocumentHeader header,
            DocumentDetails detail,
            ActionTypeForReport actionType,
            String status,
            Employee actor,
            HttpServletRequest request,
            Map<String, Object> extra
    );

    public List<DocumentActivityReportResponse> search(
            DocumentActivityReportRequest request);
}
