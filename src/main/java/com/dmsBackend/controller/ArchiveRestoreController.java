package com.dmsBackend.controller;


import com.dmsBackend.response.ArchiveRestoreDTO;
import com.dmsBackend.service.ArchiveRestoreService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/restore")
@Slf4j
public class ArchiveRestoreController {
    private final ArchiveRestoreService archiveRestoreService;

    public ArchiveRestoreController(ArchiveRestoreService archiveRestoreService) {
        this.archiveRestoreService = archiveRestoreService;
    }

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public void restoreArchive(
            @RequestParam("file") MultipartFile file,
            @RequestParam("branchId") Integer branchId,
            @RequestParam(value = "departmentId", required = false) Integer departmentId,
            @RequestParam("userRole") String userRole,
            HttpServletResponse response) {

        log.info("Starting archive restoration for file: {}, branchId: {}, departmentId: {}, userRole: {}",
                file.getOriginalFilename(), branchId, departmentId, userRole);

        try {
            if (!file.getOriginalFilename().toLowerCase().endsWith(".zip")) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            archiveRestoreService.restoreArchive(new ArchiveRestoreDTO(
                    branchId,
                    departmentId,
                    userRole,
                    file
            ), response);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            log.error("Error restoring archive: ", e);
        }
    }

    @PostMapping(value = "/all", consumes = "multipart/form-data")
    public void restoreAllArchive(
            @RequestParam("file") MultipartFile file,
            HttpServletResponse response) {

        log.info("Starting restoration of complete archive: {}", file.getOriginalFilename());

        try {
            if (!file.getOriginalFilename().toLowerCase().endsWith(".zip")) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            // Create DTO with null branch and department IDs for full admin access
            archiveRestoreService.restoreArchive(new ArchiveRestoreDTO(
                    null,
                    null,
                    "ADMIN",
                    file
            ), response);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            log.error("Error restoring complete archive: ", e);
        }
    }

    @ExceptionHandler(Exception.class)
    public void handleException(Exception e, HttpServletResponse response) {
        log.error("Unexpected error in archive restore controller: ", e);
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
}

