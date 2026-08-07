package com.dmsBackend.controller;

import com.dmsBackend.constants.ReportConstants;
import com.dmsBackend.response.DocumentActivityReportRequest;
import com.dmsBackend.response.DocumentActivityReportResponse;
import com.dmsBackend.service.DocumentActivityReportService;
import com.dmsBackend.utils.JasperReportUtil;
import com.dmsBackend.utils.ResponseUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JasperPrint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
@Tag(name = "ReportController", description = "Controller for handling All Reports")
@RequestMapping("/jasper-report")
@Slf4j
public class JasperReportController {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private DocumentActivityReportService darService;

    private Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SEARCH
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/search")
    public List<DocumentActivityReportResponse> search(
            @RequestBody DocumentActivityReportRequest request) {
        return darService.search(request);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ARCHIVED FILES  (flag: D = view PDF, P = print, E = export Excel)
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping(value = "/archived-files")
    public ResponseEntity<?> viewDownloadArchiveFile(
            @RequestParam(required = false) Integer branchId,
            @RequestParam(required = false) Integer departmentId,
            @RequestParam(required = false) Integer employeeId,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date toDate,
            @RequestParam String actionType,
            @RequestParam String flag) {

        Map<String, Object> params = buildParams(branchId, departmentId, employeeId,
                categoryId, fromDate, toDate, actionType);
        try {
            if ("D".equalsIgnoreCase(flag)) {
                byte[] pdf = JasperReportUtil.generateAndViewPdfReport(
                        ReportConstants.BASE_PATH, ReportConstants.ARCHIVED_FILES_JASPER,
                        params, getConnection());
                return buildPdfResponse(pdf, ReportConstants.ARCHIVED_FILES_REPORT);

            } else if ("P".equalsIgnoreCase(flag)) {
                JasperPrint jp = JasperReportUtil.getJasperPrintObject(
                        ReportConstants.BASE_PATH, ReportConstants.ARCHIVED_FILES_JASPER,
                        params, getConnection());
                JasperReportUtil.printJasperReport(jp);
                return ResponseEntity.ok().build();

            } else if ("E".equalsIgnoreCase(flag)) {
                return JasperReportUtil.generateExcelReport(
                        ReportConstants.BASE_PATH, ReportConstants.ARCHIVED_FILES_JASPER,
                        ReportConstants.ARCHIVED_FILES_REPORT, params, getConnection());

            } else {
                return ResponseEntity.badRequest()
                        .body(ResponseUtils.createNotFoundResponse(
                                "Invalid flag value. Use D, P, or E", 400));
            }
        } catch (Exception e) {
            log.error("Failed to generate Archived files report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to generate Archived files report: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AUDIT FILES
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping(value = "/audit-files")
    public ResponseEntity<?> viewDownloadAuditFile(
            @RequestParam(required = false) Integer branchId,
            @RequestParam(required = false) Integer departmentId,
            @RequestParam(required = false) Integer employeeId,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date toDate,
            @RequestParam String actionType,
            @RequestParam String flag) {

        Map<String, Object> params = buildParams(branchId, departmentId, employeeId,
                categoryId, fromDate, toDate, actionType);
        try {
            if ("D".equalsIgnoreCase(flag)) {
                byte[] pdf = JasperReportUtil.generateAndViewPdfReport(
                        ReportConstants.BASE_PATH, ReportConstants.AUDIT_FILES_JASPER,
                        params, getConnection());
                return buildPdfResponse(pdf, ReportConstants.AUDIT_FILES_REPORTS);

            } else if ("P".equalsIgnoreCase(flag)) {
                JasperPrint jp = JasperReportUtil.getJasperPrintObject(
                        ReportConstants.BASE_PATH, ReportConstants.AUDIT_FILES_JASPER,
                        params, getConnection());
                JasperReportUtil.printJasperReport(jp);
                return ResponseEntity.ok().build();

            } else if ("E".equalsIgnoreCase(flag)) {
                return JasperReportUtil.generateExcelReport(
                        ReportConstants.BASE_PATH, ReportConstants.AUDIT_FILES_JASPER,
                        ReportConstants.AUDIT_FILES_REPORTS, params, getConnection());

            } else {
                return ResponseEntity.badRequest()
                        .body(ResponseUtils.createNotFoundResponse(
                                "Invalid flag value. Use D, P, or E", 400));
            }
        } catch (Exception e) {
            log.error("Failed to generate Audit files report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to generate Audit files report: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DOWNLOADED FILES
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping(value = "/downloaded-files")
    public ResponseEntity<?> viewDownloadDownloadedFile(
            @RequestParam(required = false) Integer branchId,
            @RequestParam(required = false) Integer departmentId,
            @RequestParam(required = false) Integer employeeId,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date toDate,
            @RequestParam String actionType,
            @RequestParam String flag) {

        Map<String, Object> params = buildParams(branchId, departmentId, employeeId,
                categoryId, fromDate, toDate, actionType);
        try {
            if ("D".equalsIgnoreCase(flag)) {
                byte[] pdf = JasperReportUtil.generateAndViewPdfReport(
                        ReportConstants.BASE_PATH, ReportConstants.DOWNLOAD_FILES_JASPER,
                        params, getConnection());
                return buildPdfResponse(pdf, ReportConstants.DOWNLOAD_FILES_REPORT);

            } else if ("P".equalsIgnoreCase(flag)) {
                JasperPrint jp = JasperReportUtil.getJasperPrintObject(
                        ReportConstants.BASE_PATH, ReportConstants.DOWNLOAD_FILES_JASPER,
                        params, getConnection());
                JasperReportUtil.printJasperReport(jp);
                return ResponseEntity.ok().build();

            } else if ("E".equalsIgnoreCase(flag)) {
                return JasperReportUtil.generateExcelReport(
                        ReportConstants.BASE_PATH, ReportConstants.DOWNLOAD_FILES_JASPER,
                        ReportConstants.DOWNLOAD_FILES_REPORT, params, getConnection());

            } else {
                return ResponseEntity.badRequest()
                        .body(ResponseUtils.createNotFoundResponse(
                                "Invalid flag value. Use D, P, or E", 400));
            }
        } catch (Exception e) {
            log.error("Failed to generate Downloaded files report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to generate Downloaded files report: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RETRIEVED FILES
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping(value = "/retrieved-files")
    public ResponseEntity<?> viewDownloadRetrieveFile(
            @RequestParam(required = false) Integer branchId,
            @RequestParam(required = false) Integer departmentId,
            @RequestParam(required = false) Integer employeeId,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date toDate,
            @RequestParam String actionType,
            @RequestParam String flag) {

        Map<String, Object> params = buildParams(branchId, departmentId, employeeId,
                categoryId, fromDate, toDate, actionType);
        try {
            if ("D".equalsIgnoreCase(flag)) {
                byte[] pdf = JasperReportUtil.generateAndViewPdfReport(
                        ReportConstants.BASE_PATH, ReportConstants.RETRIEVE_FILES_JASPER,
                        params, getConnection());
                return buildPdfResponse(pdf, ReportConstants.RETRIEVE_FILES_REPORT);

            } else if ("P".equalsIgnoreCase(flag)) {
                JasperPrint jp = JasperReportUtil.getJasperPrintObject(
                        ReportConstants.BASE_PATH, ReportConstants.RETRIEVE_FILES_JASPER,
                        params, getConnection());
                JasperReportUtil.printJasperReport(jp);
                return ResponseEntity.ok().build();

            } else if ("E".equalsIgnoreCase(flag)) {
                return JasperReportUtil.generateExcelReport(
                        ReportConstants.BASE_PATH, ReportConstants.RETRIEVE_FILES_JASPER,
                        ReportConstants.RETRIEVE_FILES_REPORT, params, getConnection());

            } else {
                return ResponseEntity.badRequest()
                        .body(ResponseUtils.createNotFoundResponse(
                                "Invalid flag value. Use D, P, or E", 400));
            }
        } catch (Exception e) {
            log.error("Failed to generate Retrieved files report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to generate Retrieved files report: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TRASHED FILES
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping(value = "/trashed-files")
    public ResponseEntity<?> viewDownloadTrashFile(
            @RequestParam(required = false) Integer branchId,
            @RequestParam(required = false) Integer departmentId,
            @RequestParam(required = false) Integer employeeId,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date toDate,
            @RequestParam String actionType,
            @RequestParam String flag) {

        Map<String, Object> params = buildParams(branchId, departmentId, employeeId,
                categoryId, fromDate, toDate, actionType);
        try {
            if ("D".equalsIgnoreCase(flag)) {
                byte[] pdf = JasperReportUtil.generateAndViewPdfReport(
                        ReportConstants.BASE_PATH, ReportConstants.TRASH_FILES_JASPER,
                        params, getConnection());
                return buildPdfResponse(pdf, ReportConstants.TRASH_FILES_REPORT);

            } else if ("P".equalsIgnoreCase(flag)) {
                JasperPrint jp = JasperReportUtil.getJasperPrintObject(
                        ReportConstants.BASE_PATH, ReportConstants.TRASH_FILES_JASPER,
                        params, getConnection());
                JasperReportUtil.printJasperReport(jp);
                return ResponseEntity.ok().build();

            } else if ("E".equalsIgnoreCase(flag)) {
                return JasperReportUtil.generateExcelReport(
                        ReportConstants.BASE_PATH, ReportConstants.TRASH_FILES_JASPER,
                        ReportConstants.TRASH_FILES_REPORT, params, getConnection());

            } else {
                return ResponseEntity.badRequest()
                        .body(ResponseUtils.createNotFoundResponse(
                                "Invalid flag value. Use D, P, or E", 400));
            }
        } catch (Exception e) {
            log.error("Failed to generate Trash files report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to generate Trash files report: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPLOADED FILES
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping(value = "/uploaded-files")
    public ResponseEntity<?> viewDownloadUploadFile(
            @RequestParam(required = false) Integer branchId,
            @RequestParam(required = false) Integer departmentId,
            @RequestParam(required = false) Integer employeeId,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date toDate,
            @RequestParam String actionType,
            @RequestParam String flag) {

        Map<String, Object> params = buildParams(branchId, departmentId, employeeId,
                categoryId, fromDate, toDate, actionType);
        try {
            if ("D".equalsIgnoreCase(flag)) {
                byte[] pdf = JasperReportUtil.generateAndViewPdfReport(
                        ReportConstants.BASE_PATH, ReportConstants.UPLOAD_FILES_JASPER,
                        params, getConnection());
                return buildPdfResponse(pdf, ReportConstants.UPLOAD_FILES_REPORT);

            } else if ("P".equalsIgnoreCase(flag)) {
                JasperPrint jp = JasperReportUtil.getJasperPrintObject(
                        ReportConstants.BASE_PATH, ReportConstants.UPLOAD_FILES_JASPER,
                        params, getConnection());
                JasperReportUtil.printJasperReport(jp);
                return ResponseEntity.ok().build();

            } else if ("E".equalsIgnoreCase(flag)) {
                return JasperReportUtil.generateExcelReport(
                        ReportConstants.BASE_PATH, ReportConstants.UPLOAD_FILES_JASPER,
                        ReportConstants.UPLOAD_FILES_REPORT, params, getConnection());

            } else {
                return ResponseEntity.badRequest()
                        .body(ResponseUtils.createNotFoundResponse(
                                "Invalid flag value. Use D, P, or E", 400));
            }
        } catch (Exception e) {
            log.error("Failed to generate Uploaded files report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to generate Uploaded files report: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Builds the common parameter map shared by all report endpoints.
     */
    private Map<String, Object> buildParams(
            Integer branchId, Integer departmentId, Integer employeeId,
            Integer categoryId, Date fromDate, Date toDate, String actionType) {

        Map<String, Object> params = new HashMap<>();
        params.put("branch_id",     branchId);
        params.put("department_id", departmentId);
        params.put("employee_id",   employeeId);
        params.put("category_id",   categoryId);
        params.put("from_date",     fromDate);
        params.put("to_date",       toDate);
        params.put("action_type",   actionType);
        params.put("path", getClass()
                .getResource(ReportConstants.ASSET_LOGO)
                .toString());
        return params;
    }

    private ResponseEntity<byte[]> buildPdfResponse(byte[] pdfData, String fileName) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + fileName + ".pdf\"")
                .body(pdfData);
    }

    private ResponseEntity<byte[]> buildExcelResponse(byte[] excelData, String fileName) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileName + ".xlsx\"")
                .body(excelData);
    }
}