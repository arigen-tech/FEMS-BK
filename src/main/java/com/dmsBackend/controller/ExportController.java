package com.dmsBackend.controller;

import com.dmsBackend.service.ExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/export")
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;

    @GetMapping("/database")
    public ResponseEntity<byte[]> exportDatabase(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

        String exportId = UUID.randomUUID().toString();
        log.info("[{}] === DATABASE EXPORT REQUEST STARTED ===", exportId);
        log.info("[{}] Date Range - From: {}, To: {}", exportId, fromDate, toDate);

        try {
            validateDateRange(fromDate, toDate);

            log.info("[{}] Starting database export process...", exportId);
            File exportFile = exportService.exportDatabaseToCSV(fromDate, toDate, exportId);

            if (exportFile == null || !exportFile.exists()) {
                throw new IllegalStateException("Export file was not created");
            }

            byte[] fileContent = Files.readAllBytes(exportFile.toPath());

            log.info("[{}] ✅ DATABASE EXPORT COMPLETED SUCCESSFULLY", exportId);
            log.info("[{}] File: {} ({} bytes)", exportId, exportFile.getName(), fileContent.length);

            // Cleanup file after sending
            boolean deleted = exportFile.delete();
            if (!deleted) {
                log.warn("[{}] Could not delete temporary export file: {}", exportId, exportFile.getAbsolutePath());
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + exportFile.getName() + "\"")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                    .header("X-Export-ID", exportId)
                    .header("X-Export-Time", LocalDateTime.now().toString())
                    .header("X-Date-Range", formatDateRangeHeader(fromDate, toDate))
                    .header("X-File-Size", String.valueOf(fileContent.length))
                    .body(fileContent);

        } catch (IllegalStateException e) {
            log.warn("[{}] ⚠️ EXPORT BLOCKED: {}", exportId, e.getMessage());
            return ResponseEntity.status(429)
                    .header("X-Export-ID", exportId)
                    .header("X-Export-Error", "Export blocked")
                    .body(("Export blocked: " + e.getMessage()).getBytes());

        } catch (Exception e) {
            log.error("[{}] ❌ DATABASE EXPORT FAILED: {}", exportId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .header("X-Export-Error", e.getMessage())
                    .header("X-Export-ID", exportId)
                    .build();
        } finally {
            log.info("[{}] === DATABASE EXPORT REQUEST COMPLETED ===", exportId);
        }
    }

    @GetMapping("/files")
    public ResponseEntity<byte[]> exportFiles(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

        String exportId = UUID.randomUUID().toString();
        log.info("[{}] === FILES EXPORT REQUEST STARTED ===", exportId);
        log.info("[{}] Date Range - From: {}, To: {}", exportId, fromDate, toDate);

        try {
            validateDateRange(fromDate, toDate);

            log.info("[{}] Starting files export process...", exportId);
            File exportFile = exportService.exportFilesToZip(fromDate, toDate, exportId);

            if (exportFile == null || !exportFile.exists()) {
                throw new IllegalStateException("Files export file was not created");
            }

            byte[] fileContent = Files.readAllBytes(exportFile.toPath());

            log.info("[{}] ✅ FILES EXPORT COMPLETED SUCCESSFULLY", exportId);
            log.info("[{}] File: {} ({} bytes)", exportId, exportFile.getName(), fileContent.length);

            // Cleanup file after sending
            boolean deleted = exportFile.delete();
            if (!deleted) {
                log.warn("[{}] Could not delete temporary export file: {}", exportId, exportFile.getAbsolutePath());
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + exportFile.getName() + "\"")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                    .header("X-Export-ID", exportId)
                    .header("X-Export-Time", LocalDateTime.now().toString())
                    .header("X-Date-Range", formatDateRangeHeader(fromDate, toDate))
                    .header("X-File-Size", String.valueOf(fileContent.length))
                    .body(fileContent);

        } catch (IllegalStateException e) {
            log.warn("[{}] ⚠️ EXPORT BLOCKED: {}", exportId, e.getMessage());
            return ResponseEntity.status(429)
                    .header("X-Export-ID", exportId)
                    .header("X-Export-Error", "Export in progress")
                    .body(("Export blocked: " + e.getMessage()).getBytes());

        } catch (Exception e) {
            log.error("[{}] ❌ FILES EXPORT FAILED: {}", exportId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .header("X-Export-Error", e.getMessage())
                    .header("X-Export-ID", exportId)
                    .build();
        } finally {
            log.info("[{}] === FILES EXPORT REQUEST COMPLETED ===", exportId);
        }
    }

    @GetMapping("/complete")
    public ResponseEntity<byte[]> exportComplete(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

        String exportId = UUID.randomUUID().toString();
        log.info("[{}] === COMPLETE SYSTEM EXPORT REQUEST STARTED ===", exportId);
        log.info("[{}] Date Range - From: {}, To: {}", exportId, fromDate, toDate);

        try {
            validateDateRange(fromDate, toDate);

            log.info("[{}] Starting complete system export process...", exportId);
            File exportFile = exportService.exportCompleteSystem(fromDate, toDate, exportId);

            if (exportFile == null || !exportFile.exists()) {
                throw new IllegalStateException("Complete system export file was not created");
            }

            byte[] fileContent = Files.readAllBytes(exportFile.toPath());

            log.info("[{}] ✅ COMPLETE SYSTEM EXPORT COMPLETED SUCCESSFULLY", exportId);
            log.info("[{}] File: {} ({} bytes)", exportId, exportFile.getName(), fileContent.length);

            // Cleanup file after sending
            boolean deleted = exportFile.delete();
            if (!deleted) {
                log.warn("[{}] Could not delete temporary export file: {}", exportId, exportFile.getAbsolutePath());
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + exportFile.getName() + "\"")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                    .header("X-Export-ID", exportId)
                    .header("X-Export-Time", LocalDateTime.now().toString())
                    .header("X-Date-Range", formatDateRangeHeader(fromDate, toDate))
                    .header("X-File-Size", String.valueOf(fileContent.length))
                    .body(fileContent);

        } catch (IllegalStateException e) {
            log.warn("[{}] ⚠️ EXPORT BLOCKED: {}", exportId, e.getMessage());
            return ResponseEntity.status(429)
                    .header("X-Export-ID", exportId)
                    .header("X-Export-Error", "Export in progress")
                    .body(("Export blocked: " + e.getMessage()).getBytes());

        } catch (Exception e) {
            log.error("[{}] ❌ COMPLETE SYSTEM EXPORT FAILED: {}", exportId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .header("X-Export-Error", e.getMessage())
                    .header("X-Export-ID", exportId)
                    .build();
        } finally {
            log.info("[{}] === COMPLETE SYSTEM EXPORT REQUEST COMPLETED ===", exportId);
        }
    }

    @GetMapping("/history")
    public ResponseEntity<?> getExportHistory(
            @RequestParam(required = false) String type) {
        try {
            if (type != null && !type.isEmpty()) {
                return ResponseEntity.ok(exportService.getExportHistoryByTypeForFrontend(type));
            } else {
                return ResponseEntity.ok(exportService.getExportHistoryForFrontend());
            }
        } catch (Exception e) {
            log.error("Error getting export history: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        log.info("Health check requested");
        return ResponseEntity.ok("DMS Export Service is running - " + LocalDateTime.now());
    }

    private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("From date cannot be after To date");
        }

        LocalDate today = LocalDate.now();
        if (fromDate != null && fromDate.isAfter(today)) {
            throw new IllegalArgumentException("From date cannot be in the future");
        }
        if (toDate != null && toDate.isAfter(today)) {
            throw new IllegalArgumentException("To date cannot be in the future");
        }
    }

    private String formatDateRangeHeader(LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null) {
            return fromDate + "_to_" + toDate;
        } else if (fromDate != null) {
            return "from_" + fromDate;
        } else if (toDate != null) {
            return "to_" + toDate;
        }
        return "all_dates";
    }
}