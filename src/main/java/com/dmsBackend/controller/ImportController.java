package com.dmsBackend.controller;

import com.dmsBackend.response.ImportResponse;
import com.dmsBackend.service.ImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/import")
@RequiredArgsConstructor
//@CrossOrigin(origins = "*")
public class ImportController {

    private final ImportService importService;

    @PostMapping("/validate")
    public ResponseEntity<ImportResponse> validateImportFile(
            @RequestParam("file") MultipartFile file) {

        log.info("Validating import file: {}", file.getOriginalFilename());

        try {
            ImportResponse response = importService.validateImportFile(file);
            log.info("File validation successful: {}", file.getOriginalFilename());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("File validation failed: {}", e.getMessage(), e);
            ImportResponse errorResponse = new ImportResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("Validation error: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/restore")
    public ResponseEntity<ImportResponse> importDMSExport(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "importDatabase", defaultValue = "true") boolean importDatabase,
            @RequestParam(value = "importFiles", defaultValue = "true") boolean importFiles,
            @RequestParam(value = "overwriteExisting", defaultValue = "false") boolean overwriteExisting,
            @RequestParam(value = "selectedTables", required = false) String selectedTables,
            @RequestParam(value = "selectedFiles", required = false) String selectedFiles) {

        String importId = UUID.randomUUID().toString();

        log.info(
                "Starting DMS import [ID: {}] | DB: {} | Files: {} | Overwrite: {} | Tables: {} | Files: {}",
                importId, importDatabase, importFiles, overwriteExisting, selectedTables, selectedFiles
        );

        try {
            Set<String> tablesSet = new HashSet<>();
            if (selectedTables != null && !selectedTables.trim().isEmpty()) {
                tablesSet.addAll(Arrays.asList(selectedTables.split(",")));
                log.info("Parsed selected tables: {}", tablesSet);
            }

            Set<String> filesSet = new HashSet<>();
            if (selectedFiles != null && !selectedFiles.trim().isEmpty()) {
                filesSet.addAll(Arrays.asList(selectedFiles.split(",")));
                log.info("Parsed selected files: {}", filesSet);
            }

            ImportResponse response = importService.importDMSExport(
                    file, importDatabase, importFiles, overwriteExisting, tablesSet, filesSet
            );

            if (response.isSuccess()) {
                log.info("DMS import completed successfully [ID: {}]", importId);
                return ResponseEntity.ok(response);
            } else {
                log.error("DMS import failed [ID: {}]: {}", importId, response.getMessage());
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            log.error("DMS import exception [ID: {}]: {}", importId, e.getMessage(), e);
            ImportResponse errorResponse = new ImportResponse();
            errorResponse.setImportId(importId);
            errorResponse.setSuccess(false);
            errorResponse.setMessage("Import failed: " + e.getMessage());
            errorResponse.setError(e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping("/database-only")
    public ResponseEntity<ImportResponse> importDatabaseOnly(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "overwriteExisting", defaultValue = "false") boolean overwriteExisting,
            @RequestParam(value = "selectedTables", required = false) String selectedTables) {

        Set<String> tablesSet = new HashSet<>();
        if (selectedTables != null && !selectedTables.trim().isEmpty()) {
            tablesSet.addAll(Arrays.asList(selectedTables.split(",")));
        }

        log.info("Database-only import started | Tables: {} | Overwrite: {}", tablesSet, overwriteExisting);

        return ResponseEntity.ok(
                importService.importDMSExport(file, true, false, overwriteExisting, tablesSet, new HashSet<>())
        );
    }

    @PostMapping("/files-only")
    public ResponseEntity<ImportResponse> importFilesOnly(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "overwriteExisting", defaultValue = "false") boolean overwriteExisting,
            @RequestParam(value = "selectedFiles", required = false) String selectedFiles) {

        Set<String> filesSet = new HashSet<>();
        if (selectedFiles != null && !selectedFiles.trim().isEmpty()) {
            filesSet.addAll(Arrays.asList(selectedFiles.split(",")));
        }

        log.info("Files-only import started | Files: {} | Overwrite: {}", filesSet, overwriteExisting);

        return ResponseEntity.ok(
                importService.importDMSExport(file, false, true, overwriteExisting, new HashSet<>(), filesSet)
        );
    }

    @GetMapping("/status/{importId}")
    public ResponseEntity<ImportResponse> getImportStatus(@PathVariable String importId) {

        log.info("Checking import status for ID: {}", importId);

        ImportResponse response = new ImportResponse();
        response.setImportId(importId);
        response.setSuccess(true);
        response.setMessage("Import service is ready");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        log.info("Health check invoked for Import Service");
        return ResponseEntity.ok("DMS Import Service is running - 100GB file limit supported");
    }
}
