package com.dmsBackend.controller;

import com.dmsBackend.ArchiveCodes.ArchiveServicesImpl;
import com.dmsBackend.ArchiveWithLTO9.LtfsRetrieveFiles;
import com.dmsBackend.P5Archive.P5RestoreApiService;
import com.dmsBackend.entity.*;

import java.io.*;

import com.dmsBackend.repository.*;
import com.dmsBackend.service.*;
import com.dmsBackend.utils.*;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.dmsBackend.response.*;
import com.dmsBackend.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.format.annotation.DateTimeFormat;

import com.dmsBackend.exception.ResourceConflictException;
import com.dmsBackend.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;


import java.time.format.DateTimeFormatter;
import java.util.*;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import java.nio.file.Path;
import java.nio.file.Paths;

//@CrossOrigin(origins = "https://happytyagi.github.io/DmsFrontend/")
//@CrossOrigin(origins = "http://localhost:3000")
@RestController
@Slf4j
@RequestMapping("/api/documents")
public class DocumentController {

    @Autowired
    private FileEncryptionUtil fileEncryptionUtil;
    @Autowired
    private DocumentHeaderService documentHeaderService;

    @Autowired
    private QRCodeGenerator qrCodeGenerator;

    @Autowired
    private LtfsRetrieveFiles ltfsRetrieveFiles;

    @Autowired
    private  DocumentActivityReportService documentActivityReportService;
    @Autowired
    private DocumentDetailsService documentDetailsService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private ArchiveServicesImpl archiveServices;

    @Autowired
    private DocumentDetailsRepository documentDetailsRepository;

    @Autowired
    private CategoryMasterRepository categoryMasterRepository;

    @Autowired
    private BranchMasterRepository branchMasterRepository;

    @Autowired
    private DepartmentMasterRepository departmentMasterRepository;

    @Autowired
    private  P5RestoreApiService restoreApiService;

    @Autowired
    private BranchMasterService branchMasterService;

    @Autowired
    private AuditLogUtil auditLogUtil;

    @Autowired
    private CurrentUser currentUser;

    @Autowired
    private DocumentHeaderRepository documentHeaderRepository;

    @Value("${document.storage.path}")
    private String documentStoragePath;

    @Value("${document.archive.path}")
    private String documentArchivePath;


    @Autowired
    DocumentsAuditLogService documentsAuditLogService;


    @Value("${document.sftp.host}") private String sftpHost;
    @Value("${document.sftp.port}") private int sftpPort;
    @Value("${document.sftp.username}") private String sftpUser;
    @Value("${document.sftp.password}") private String sftpPassword;
    @Value("${document.sftp.baseDir}") private String sftpBaseDir;


    private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);

    // ================== DocumentHeader Operations ================== //


    @PostMapping("/save")
    public ResponseEntity<ApiResponse<Map<String, Object>>> saveDocumentWithFiles(@RequestBody DocumentSaveRequest documentSaveRequest, HttpServletRequest request) {
        log.info("API CALL → Save Document With Files");
        ApiResponse<Map<String, Object>> response = documentHeaderService.saveDocumentWithFiles(documentSaveRequest,request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping("/update")
    public ResponseEntity<ApiResponse<MessageResponse>> updateDocumentWithFiles(
            @RequestBody DocumentSaveRequest requestBody,
            @RequestParam(required = false) String version,
            HttpServletRequest request) {

        log.info("API CALL → Update Document With Files | version={}", version);

        ApiResponse<MessageResponse> response =
                documentHeaderService.updateDocumentWithFiles(
                        requestBody.getDocumentHeader(),
                        requestBody.getMetadata(),
                        requestBody.getDeletedMetaDataIds(),
                        requestBody.getFilePaths(),
                        requestBody.getForwardingAuthority(),
                        version,
                        request
                );

        return ResponseEntity.ok(response);
    }




    //get all enum
    @GetMapping("/docApprovalStatuses")
    public List<Map<String, String>> getAllDocApprovalStatuses() {
        log.info("API CALL → Get All Document Approval Statuses");

        List<Map<String, String>> statuses = Arrays.stream(DocApprovalStatus.values())
                .map(status -> Map.of(status.name(), ""))
                .collect(Collectors.toList());

        log.info("SUCCESS → Retrieved {} approval statuses", statuses.size());

        return statuses;
    }

    // Find a document by ID
    @GetMapping("findBy/{id}")
    public ResponseEntity<DocumentHeader> getDocumentHeaderById(@PathVariable Integer id) {
        log.info("API CALL → Get Document By ID | id={}", id);

        try {
            DocumentHeader documentHeader = documentHeaderService.findDocumentHeaderById(id);
            log.info("SUCCESS → Document Retrieved | id={}", id);
            return ResponseEntity.ok(documentHeader); // Return 200 OK with document data
        } catch (ResourceNotFoundException e) {
            log.error("FAILED → Get Document By ID | id={} reason=Not Found", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null); // Return 404 if not found
        }
    }

    @GetMapping("findByBranchId/{id}")
    public ResponseEntity<?> getDocumentHeaderByBranchId(@PathVariable Integer id) {
        log.info("API CALL → Get Documents By Branch ID | branchId={}", id);

        List<DocumentHeader> documentHeaders = documentHeaderService.findDocumentHeaderByBranchId(id);

        if (documentHeaders == null || documentHeaders.isEmpty()) {
            log.warn("No documents found for branch ID: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No documents found for branch ID: " + id);
        }

        log.info("SUCCESS → Retrieved {} documents for branch ID: {}", documentHeaders.size(), id);
        return ResponseEntity.ok(documentHeaders);
    }

    @GetMapping("findByDepartmrntId/{id}")
    public ResponseEntity<?> getDocumentHeaderByDepartmentId(@PathVariable Integer id) {
        log.info("API CALL → Get Documents By Department ID | departmentId={}", id);

        List<DocumentHeader> documentHeaders = documentHeaderService.findDocumentHeaderByDepartmentId(id);

        if (documentHeaders == null || documentHeaders.isEmpty()) {
            log.warn("No documents found for department ID: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No documents found for Department ID: " + id);
        }

        log.info("SUCCESS → Retrieved {} documents for department ID: {}", documentHeaders.size(), id);
        return ResponseEntity.ok(documentHeaders);
    }



    // Find all documents
    @GetMapping("/getAll")
    public ResponseEntity<List<DocumentHeader>> getAllDocumentHeaders() {
        log.info("API CALL → Get All Documents");

        List<DocumentHeader> documentHeaders = documentHeaderService.findAllDocumentHeaders();

        log.info("SUCCESS → Retrieved {} documents", documentHeaders.size());

        return new ResponseEntity<>(documentHeaders, HttpStatus.OK);
    }

    // Delete a document by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocumentHeader(@PathVariable Integer id) {
        log.info("API CALL → Delete Document | id={}", id);

        documentHeaderService.deleteByIdDocumentHeader(id);

        log.info("SUCCESS → Document Deleted | id={}", id);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    // Update the approval status of a document
    // Update the approval status of a document (HEADER LEVEL)
    @PatchMapping("/{id}/approval-status")
    public ResponseEntity<DocumentHeader> updateApprovalStatus(
            @PathVariable Integer id,
            @RequestParam("status") DocApprovalStatus status,
            @RequestParam(value = "rejectionReason", required = false) String rejectionReason) {

        log.info("API CALL → Update Document Approval Status | id={} status={}", id, status);

        Integer employeeId = currentUser.getCurrentEmployeeOrThrow().getId();

        DocumentHeader updatedDocument =
                documentHeaderService.updateApprovalStatus(id, status, rejectionReason, employeeId);

        log.info("SUCCESS → Document Approval Status Updated | id={} status={}", id, status);

        return new ResponseEntity<>(updatedDocument, HttpStatus.OK);
    }
    // Update the active status of a document
    @PatchMapping("/{id}/active-status")
    public ResponseEntity<DocumentHeader> updateActiveStatus(
            @PathVariable Integer id, @RequestParam("isActive") boolean isActive) {

        log.info("API CALL → Update Document Active Status | id={} isActive={}", id, isActive);

        DocumentHeader updatedDocument = documentHeaderService.updateActiveStatus(id, isActive);

        log.info("SUCCESS → Document Active Status Updated | id={} isActive={}", id, isActive);

        return new ResponseEntity<>(updatedDocument, HttpStatus.OK);
    }

    // Get all approved documents
    @GetMapping("/approved")
    public ResponseEntity<List<DocumentHeader>> getAllApproved() {
        log.info("API CALL → Get All Approved Documents");

        List<DocumentHeader> approvedDocuments = documentHeaderService.getAllApproved();

        log.info("SUCCESS → Retrieved {} approved documents", approvedDocuments.size());

        return new ResponseEntity<>(approvedDocuments, HttpStatus.OK);
    }

    // Get all rejected documents
    @GetMapping("/rejected")
    public ResponseEntity<List<DocumentHeader>> getAllRejected() {
        log.info("API CALL → Get All Rejected Documents");

        List<DocumentHeader> rejectedDocuments = documentHeaderService.getAllRejected();

        log.info("SUCCESS → Retrieved {} rejected documents", rejectedDocuments.size());

        return new ResponseEntity<>(rejectedDocuments, HttpStatus.OK);
    }

    // Get all pending documents
    @GetMapping("/pending")
    public ResponseEntity<?> getAllPending() {
        log.info("API CALL → Get All Pending Documents");

        try {
            List<DocumentHeader> pendingDocuments = documentHeaderService.getAllPendingDocuments();
            log.info("SUCCESS → Retrieved {} pending documents", pendingDocuments.size());
            return new ResponseEntity<>(pendingDocuments, HttpStatus.OK);
        } catch (RuntimeException e) {
            log.error("FAILED → Get All Pending Documents | reason={}", e.getMessage());
            return new ResponseEntity<>("Failed to fetch pending documents. Please try again later.", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            log.error("ERROR → Get All Pending Documents | reason={}", e.getMessage());
            return new ResponseEntity<>("An unexpected error occurred. Please contact support.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    // Get all approved documents for a specific employee
    @GetMapping("/approved/employee/{employeeId}")
    public ResponseEntity<List<DocumentHeader>> getAllApprovedByEmployeeId(@PathVariable Integer employeeId) {
        log.info("API CALL → Get Approved Documents By Employee | employeeId={}", employeeId);

        List<DocumentHeader> approvedDocuments = documentHeaderService.getAllApprovedByEmployeeId(employeeId);

        log.info("SUCCESS → Retrieved {} approved documents for employee ID: {}", approvedDocuments.size(), employeeId);

        return new ResponseEntity<>(approvedDocuments, HttpStatus.OK);
    }

    // Get all rejected documents for a specific employee
    @GetMapping("/rejected/employee/{employeeId}")
    public ResponseEntity<List<DocumentHeader>> getAllRejectedByEmployeeId(@PathVariable Integer employeeId) {
        log.info("API CALL → Get Rejected Documents By Employee | employeeId={}", employeeId);

        List<DocumentHeader> rejectedDocuments = documentHeaderService.getAllRejectedByEmployeeId(employeeId);

        log.info("SUCCESS → Retrieved {} rejected documents for employee ID: {}", rejectedDocuments.size(), employeeId);

        return new ResponseEntity<>(rejectedDocuments, HttpStatus.OK);
    }

    // Get all pending documents for a specific employee
    @GetMapping("/pending/employee/{employeeId}")
    public ResponseEntity<List<DocumentHeader>> getAllPendingByEmployeeId(@PathVariable Integer employeeId) {
        log.info("API CALL → Get Pending Documents By Employee | employeeId={}", employeeId);

        List<DocumentHeader> pendingDocuments = documentHeaderService.getAllPendingByEmployeeId(employeeId);

        log.info("SUCCESS → Retrieved {} pending documents for employee ID: {}", pendingDocuments.size(), employeeId);

        return new ResponseEntity<>(pendingDocuments, HttpStatus.OK);
    }

    // Get all documents for a specific employee
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<DocumentHeader>> getAllDocumentHeadersByEmployeeId(@PathVariable Integer employeeId) {
        log.info("API CALL → Get All Documents By Employee | employeeId={}", employeeId);

        List<DocumentHeader> employeeDocuments = documentHeaderService.findAllDocumentHeadersByEmployeeId(employeeId);

        log.info("SUCCESS → Retrieved {} documents for employee ID: {}", employeeDocuments.size(), employeeId);

        return new ResponseEntity<>(employeeDocuments, HttpStatus.OK);
    }

    // Get all rejected documents updated by an employee
    @GetMapping("/rejectedByEmp")
    public ResponseEntity<List<DocumentHeader>> getRejectedByEmployee(@RequestHeader("employeeId") Integer employeeId) {
        log.info("API CALL → Get Rejected Documents By Action Employee | employeeId={}", employeeId);

        List<DocumentHeader> rejectedDocuments = documentHeaderService.findAllRejectedByActionEmployeeId(employeeId);

        log.info("SUCCESS → Retrieved {} rejected documents by action employee ID: {}", rejectedDocuments.size(), employeeId);

        return new ResponseEntity<>(rejectedDocuments, HttpStatus.OK);
    }

    // Get all approved documents updated by an employee
    @GetMapping("/approvedByEmp")
    public ResponseEntity<List<DocumentHeader>> getApprovedByEmployee(
            @RequestHeader("employeeId") Integer employeeId) {

        log.info("API CALL → Get Approved Documents By Action Employee | employeeId={}", employeeId);

        List<DocumentHeader> approvedDocuments =
                documentHeaderService.findAllApprovedByActionEmployeeId(employeeId);

        log.info("SUCCESS → Retrieved {} approved documents by action employee ID: {}", approvedDocuments.size(), employeeId);

        return ResponseEntity.ok(approvedDocuments);
    }


    @GetMapping("/byDocumentHeader/{id}/{status}")
    public ResponseEntity<List<DocumentDetailsResponse>> getDocumentsByHeader(
            @PathVariable Integer id,
            @PathVariable String status) {

        log.info("API CALL → Get Documents By Header | headerId={} status={}", id, status);

        DocApprovalStatus docStatus = null;

        if (!"ALL".equalsIgnoreCase(status)) {
            docStatus = DocApprovalStatus.valueOf(status.toUpperCase());
        }

        List<DocumentDetailsResponse> documents =
                documentDetailsService.findDocumentsByHeaderId(id, docStatus);

        log.info("SUCCESS → Retrieved {} documents for header ID: {} with status: {}", documents.size(), id, status);

        return ResponseEntity.ok(documents);
    }



    //for graph
    @GetMapping("/documents-summary/{employeeId}")
    public ResponseEntity<Map<String, Object>> getDocumentsSummaryByEmployeeId(
            @PathVariable Integer employeeId,
            @RequestParam("startDate") Timestamp startDate,
            @RequestParam("endDate") Timestamp endDate) {

        log.info("API CALL → Get Documents Summary By Employee | employeeId={} startDate={} endDate={}",
                employeeId, startDate, endDate);

        Map<String, Object> result = documentHeaderService.countAllDocumentsByIdWithMonth(employeeId, startDate, endDate);

        log.info("SUCCESS → Retrieved documents summary for employee ID: {}", employeeId);

        return ResponseEntity.ok(result);
    }


    // ================== DocumentDetails (File Upload) Operations ================== //

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFiles(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("category") String category,
            @RequestParam("year") String year,
            @RequestParam("branch") String branch,
            @RequestParam("department") String department,
            @RequestParam("version") String version,
            @RequestParam(value = "waitingRoomIds", required = false) List<Integer> waitingRoomIds) { // ✅ Add this

        log.info("API CALL → Upload Files | branch={} department={} category={} year={} version={} fileCount={}",
                branch, department, category, year, version, files.size());

        try {
            Map<String, Object> result = documentDetailsService.uploadFiles(
                    files, branch, department, category, year, version, waitingRoomIds); // ✅ Pass it

            List<Map<String, Object>> uploadedFiles = (List<Map<String, Object>>) result.get("uploadedFiles");
            List<Map<String, String>> errors = (List<Map<String, String>>) result.get("errors");

            Map<String, Object> response = new HashMap<>();
            response.put("uploadedFiles", uploadedFiles);
            response.put("errors", errors);

            if (errors.isEmpty()) {
                log.info("SUCCESS → All {} files uploaded successfully", files.size());
                return ResponseEntity.ok(response);
            } else {
                log.warn("PARTIAL SUCCESS → {} files uploaded, {} errors", uploadedFiles.size(), errors.size());
                return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).body(response);
            }
        } catch (RuntimeException e) {
            log.error("FAILED → Upload Files | reason={}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("uploadedFiles", Collections.emptyList());
            errorResponse.put("errors", List.of(Map.of("file", "unknown", "error", e.getMessage())));

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }


//   download with linux server

//    @GetMapping("/download/{branch}/{department}/{year}/{category}/{version}/{fileName}")
//    public ResponseEntity<Resource> downloadFile(
//            @PathVariable String branch,
//            @PathVariable String department,
//            @PathVariable String year,
//            @PathVariable String category,
//            @PathVariable String version,
//            @PathVariable String fileName) {
//
//        try {
//            category = URLDecoder.decode(category, StandardCharsets.UTF_8.name());
//            fileName = URLDecoder.decode(fileName, StandardCharsets.UTF_8.name());
//
//            // Remote paths (EC2)
//            String currentFilePath = String.format("%s/%s/%s/%s/%s/%s",
//                    sftpBaseDir, branch, department, year, category, version);
//
//            String archiveFilePath = String.format("%s/%s/%s/%s/%s/%s",
//                    documentArchivePath, branch, department, year, category, version);
//
//            String actualRemoteDir = null;
//
//            // 1️⃣ Try current storage first
//            if (SftpUtil.exists(sftpHost, sftpPort, sftpUser, sftpPassword, currentFilePath + "/" + fileName)) {
//                actualRemoteDir = currentFilePath;
//                System.out.println("File found in EC2 storage: " + actualRemoteDir + "/" + fileName);
//            }
//            // 2️⃣ Try archive
//            else if (SftpUtil.exists(sftpHost, sftpPort, sftpUser, sftpPassword, archiveFilePath + "/" + fileName)) {
//                actualRemoteDir = archiveFilePath;
//                System.out.println("File found in EC2 archive: " + actualRemoteDir + "/" + fileName);
//            }
//
//            if (actualRemoteDir == null) {
//                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
//            }
//
//            // Download from EC2 to temp file
//            Path tempFile = Files.createTempFile("download_", "_" + fileName);
//            try (OutputStream os = Files.newOutputStream(tempFile)) {
//                SftpUtil.download(
//                        actualRemoteDir, fileName,
//                        os,
//                        sftpHost, sftpPort, sftpUser, sftpPassword
//                );
//            }
//
//            Resource resource = new UrlResource(tempFile.toUri());
//            if (!resource.exists() || !resource.isReadable()) {
//                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
//            }
//
//            String mimeType = Files.probeContentType(tempFile);
//            if (mimeType == null) {
//                mimeType = "application/octet-stream";
//            }
//
//            return ResponseEntity.ok()
//                    .contentType(MediaType.parseMediaType(mimeType))
//                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
//                    .body(resource);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
//        }
//    }


    // with surbhi media
//    @GetMapping("/download/{branch}/{department}/{year}/{category}/{version}/{fileName}")
//    public ResponseEntity<Resource> downloadFile(
//            @PathVariable String branch,
//            @PathVariable String department,
//            @PathVariable String year,
//            @PathVariable String category,
//            @PathVariable String version,
//            @PathVariable String fileName) {
//
//        try {
//            category = URLDecoder.decode(category, StandardCharsets.UTF_8.name());
//            fileName = URLDecoder.decode(fileName, StandardCharsets.UTF_8.name());
//
//            // 🔍 Step 1: Lookup document by docName
//            DocumentDetails doc = documentDetailsRepository.findByDocName(fileName);
//
//            // ⚡ Case A: File exists on disk even if DB record missing
//            ResponseEntity<Resource> localResponse = serveFromLocal(branch, department, year, category, version, fileName);
//            if (localResponse.getStatusCode().is2xxSuccessful()) {
//                if (doc == null) {
//                    log.warn("⚠️ File '{}' not found in DB but exists locally — serving directly.", fileName);
//                }
//                return localResponse; // ✅ Serve file directly
//            }
//
//            // ⚡ Case B: DB entry present but archived → handle normally
//            if (doc != null) {
//                if (!Boolean.TRUE.equals(doc.getArchive())) {
//                    return serveFromLocal(branch, department, year, category, version, fileName);
//                }
//
//                if ("ARCHIVED".equalsIgnoreCase(doc.getArchivalStatus())) {
//                    DocumentArchive archive = doc.getDocumentArchive();
//                    if (archive == null) {
//                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
//                    }
//
//                    // ✅ Check again before restoring
//                    ResponseEntity<Resource> existingResponse = serveFromLocal(branch, department, year, category, version, fileName);
//                    if (existingResponse.getStatusCode().is2xxSuccessful()) {
//                        log.info("✅ File already restored, skipping restore API call: {}", fileName);
//                        return existingResponse;
//                    }
//
//                    // 🧩 Prepare restore payload
//                    Map<String, Object> restoreRequest = new HashMap<>();
//                    restoreRequest.put("collectionName", archive.getCollectionName());
//                    restoreRequest.put("destinationServer", archive.getSourceServer());
//                    restoreRequest.put("filePathRoot", archive.getFilePathRoot());
//                    restoreRequest.put("instance", 0);
//                    restoreRequest.put("objectName", archive.getObjectName());
//                    restoreRequest.put("options", " ");
//                    restoreRequest.put("priority", archive.getPriority() != null ? archive.getPriority() : 50);
//                    restoreRequest.put("qos", 0);
//
//                    boolean restored = archiveServices.restoreFile(restoreRequest);
//
//                    if (restored) {
//                        documentDetailsRepository.markRestored(archive);
//
//                        // ⏳ Retry check until file available
//                        int maxAttempts = 5;
//                        for (int attempt = 0; attempt < maxAttempts; attempt++) {
//                            ResponseEntity<Resource> fileResponse =
//                                    serveFromLocal(branch, department, year, category, version, fileName);
//                            if (fileResponse.getStatusCode().is2xxSuccessful()) {
//                                return fileResponse;
//                            }
//                            Thread.sleep(2000);
//                        }
//
//                        return ResponseEntity.status(HttpStatus.ACCEPTED).body(null); // still processing
//                    } else {
//                        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(null);
//                    }
//                }
//            }
//
//            // ❌ If neither DB nor file found
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
//
//        } catch (Exception e) {
//            log.error("❌ Download failed: {}", e.getMessage(), e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
//        }
//    }
//


//    with p5 lto

    @GetMapping("/download/{branch}/{department}/{year}/{category}/{version}/{fileName}")
    public ResponseEntity<?> downloadFile(
            @PathVariable String branch,
            @PathVariable String department,
            @PathVariable String year,
            @PathVariable String category,
            @PathVariable String version,
            @PathVariable String fileName,
            @RequestParam(defaultValue = "download") String action,
            HttpServletRequest request) {

        log.info("API CALL → Download/View File | fileName={} action={} branch={} department={}",
                fileName, action, branch, department);

        try {
            category = URLDecoder.decode(category, StandardCharsets.UTF_8.name());
            fileName = URLDecoder.decode(fileName, StandardCharsets.UTF_8.name());
            System.out.println("file"+fileName);
            DocumentDetails doc = documentDetailsRepository.findByDocName(fileName);

            // Try local file first
            ResponseEntity<Resource> localResponse =
                    serveFromLocal(branch, department, year, category, version, fileName);

            if (localResponse.getStatusCode().is2xxSuccessful()) {
                log.info("SUCCESS → File served from local storage | fileName={}", fileName);
                logAuditIfNeeded(doc, action, request); // Pass the action parameter
                return localResponse;
            }

            if (doc != null) {
                boolean isArchived = Boolean.TRUE.equals(doc.getArchive());
                boolean isRestored = Boolean.TRUE.equals(doc.getRestored());

                if (isArchived && !isRestored) {
                    log.info("File is archived, initiating restore | fileName={}", fileName);
                    String resp=ltfsRetrieveFiles.restoreFile(doc.getId());
                    if(resp.length()>0){
                       return ResponseEntity.status(HttpStatus.ACCEPTED)
                                .body(resp);
                    }
                    return ResponseEntity.status(HttpStatus.ACCEPTED)
                            .body("Restore request is accepted, please wait, it may take some time.");
                }

                ResponseEntity<Resource> response =
                        serveFromLocal(branch, department, year, category, version, fileName);

                if (response.getStatusCode().is2xxSuccessful()) {
                    log.info("SUCCESS → File served after restore check | fileName={}", fileName);
                    logAuditIfNeeded(doc, action, request); // Pass the action parameter
                }

                return response;
            }

            log.warn("File not found | fileName={}", fileName);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File not found");

        } catch (Exception e) {
            log.error("FAILED → Download/View File | fileName={} reason={}", fileName, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal server error");
        }
    }



    private void logAuditIfNeeded(DocumentDetails doc,
                                  String action,
                                  HttpServletRequest request) {

        if (doc == null) return;

        Employee employee = currentUser.getCurrentEmployeeOrThrow();

        ActionTypeForReport actionType =
                action.equalsIgnoreCase("view")
                        ? ActionTypeForReport.VIEW
                        : ActionTypeForReport.DOWNLOAD;

        documentActivityReportService.logAction(
                doc.getDocumentHeader(),
                doc,
                actionType,
                "SUCCESS",
                employee,
                request,
                Map.of(
                        "version", doc.getVersion(),
                        "fileSize", doc.getFileSizeHuman()
                )
        );


        DocumentDetailsResponse file = new DocumentDetailsResponse();
        file.setId(doc.getId());
        file.setDocName(doc.getDocName());

        // Use the actual action parameter passed from frontend
        String activity = action.equalsIgnoreCase("view")
                ? "View"
                : "Download"; // Default to "Download" if not "view"

        auditLogUtil.logDocumentAction(
                employee,
                "Document",
                activity, // Now this will be "View" or "Download" based on frontend action
                "Success",
                doc.getDocumentHeader().getId(),
                List.of(file),
                Map.of(
                        "action", activity,
                        "fileSize", doc.getFileSizeBytes(),
                        "version", doc.getVersion(),
                        "fileSizeHuman", doc.getFileSizeHuman()
                ),
                request
        );
    }

    //helper for download
//Without Decryption
//    private ResponseEntity<Resource> serveFromLocal(String branch, String department,
//                                                    String year, String category,
//                                                    String version, String fileName) throws IOException {
//        String currentFilePath = String.format("%s/%s/%s/%s/%s/%s/%s",
//                documentStoragePath, branch, department, year, category, version, fileName);
//
//        Path filePath = Paths.get(currentFilePath);
//
//        if (!Files.exists(filePath)) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
//        }
//
//        Resource resource = new UrlResource(filePath.toUri());
//        if (!resource.exists() || !resource.isReadable()) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
//        }
//
//        String mimeType = Files.probeContentType(filePath);
//        if (mimeType == null) mimeType = "application/octet-stream";
//
//        return ResponseEntity.ok()
//                .contentType(MediaType.parseMediaType(mimeType))
//                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
//                .body(resource);
//    }

    //Send Decrypted file
    private ResponseEntity<Resource> serveFromLocal(String branch, String department,
                                                    String year, String category,
                                                    String version, String fileName) throws IOException {

        log.debug("Serving file from local storage | fileName={}", fileName);

        Path filePath = Paths.get(
                documentStoragePath, branch, department, year, category, version, fileName
        );

        if (!Files.exists(filePath)) {
            log.debug("File not found in local storage | path={}", filePath);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        InputStream encryptedStream = Files.newInputStream(filePath);
        InputStream decryptedStream;
        try {
            decryptedStream = fileEncryptionUtil.decrypt(encryptedStream);
        } catch (Exception e) {
            log.error("Decryption failed | fileName={} reason={}", fileName, e.getMessage());
            throw new IOException("Decryption failed", e);
        }

        Resource resource = new InputStreamResource(decryptedStream);

        String mimeType = Files.probeContentType(filePath);
        if (mimeType == null) mimeType = "application/octet-stream";

        log.debug("File served successfully | fileName={} mimeType={}", fileName, mimeType);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mimeType))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileName + "\""
                )
                .body(resource);
    }



    @GetMapping("/pendingByBranch/{branchId}/{departmentId}")
    public ResponseEntity<List<DocumentHeader>> getPendingDocumentsByScope(
            @PathVariable Integer branchId,
            @PathVariable(required = false) Integer departmentId,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("API CALL → Get Pending Documents By Scope | branchId={} departmentId={}", branchId, departmentId);

        if (hasRole(userDetails, "SYSTEM ADMIN")) {
            log.debug("System Admin access - returning all pending documents");
            return ResponseEntity.ok(documentHeaderService.getAllPendingDocuments());

        } else if (hasRole(userDetails, "LABORATORY ADMIN / HOD")) {
            log.debug("Lab Admin/Director access - returning pending documents for branch: {}", branchId);
            return ResponseEntity.ok(documentHeaderService.getPendingDocumentsByBranch(branchId));

        } else if (hasRole(userDetails, "SCIENTIFIC OFFICER") || hasRole(userDetails, "CASE & EVIDENCE OFFICER")) {
            if (departmentId == null) {
                log.warn("Officer access but departmentId is null");
                return ResponseEntity.badRequest().body(Collections.emptyList());
            }
            log.debug("Officer access - returning pending documents for department: {}", departmentId);
            return ResponseEntity.ok(documentHeaderService.getPendingDocumentsByDepartment(departmentId));
        }

        log.warn("Access denied - user doesn't have required role");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Collections.emptyList());
    }


    private boolean hasRole(UserDetails userDetails, String role) {
        return userDetails.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase(role));
    }


    @GetMapping("/pendingByBranch/{branchId}")
    public ResponseEntity<List<DocumentHeader>> getPendingDocumentsByBranchOnly(
            @PathVariable Integer branchId,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("API CALL → Get Pending Documents By Branch Only | branchId={}", branchId);

        List<DocumentHeader> pendingDocuments;

        if (hasRole(userDetails, "SYSTEM ADMIN")) {
            log.debug("System Admin access - returning all pending documents");
            pendingDocuments = documentHeaderService.getAllPendingDocuments();
        } else if (hasRole(userDetails, "LABORATORY ADMIN / HOD")) {
            log.debug("Lab Admin/Director access - returning pending documents for branch: {}", branchId);
            pendingDocuments = documentHeaderService.getPendingDocumentsByBranch(branchId);
        } else {
            log.warn("Access denied - user doesn't have required role");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Collections.emptyList());
        }

        log.info("SUCCESS → Retrieved {} pending documents", pendingDocuments.size());
        return ResponseEntity.ok(pendingDocuments);
    }


    @GetMapping("/approvedByBranch/{employeeId}")
    public ResponseEntity<List<DocumentHeader>> getApprovedDocumentsByBranchAdmin(@PathVariable Integer employeeId) {
        log.info("API CALL → Get Approved Documents By Branch Admin | employeeId={}", employeeId);

        List<DocumentHeader> approvedDocuments = documentHeaderService.findApprovedDocumentsByBranchAdmin(employeeId);

        log.info("SUCCESS → Retrieved {} approved documents for branch admin", approvedDocuments.size());

        return ResponseEntity.ok(approvedDocuments);
    }


    @PostMapping("/search")
    public List<DocumentHeader> searchDocuments(
            @RequestBody SearchCriteria searchCriteria) {

        log.info("API CALL → Search Documents | criteria={}", searchCriteria);

        List<DocumentHeader> results = documentHeaderService.searchDocuments(searchCriteria);

        log.info("SUCCESS → Found {} documents matching search criteria", results.size());

        return results;
    }


    //for user
    @GetMapping("/document/summary/by/{employeeId}")
    public ResponseEntity<Map<String, Object>> getDocumentSummaryByEmployee(
            @PathVariable Integer employeeId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate) {

        log.info("API CALL → Get Document Summary By Employee | employeeId={} startDate={} endDate={}",
                employeeId, startDate, endDate);

        Map<String, Object> summary = documentHeaderService.getApprovalSummaryByEmployeeId(employeeId, startDate, endDate);

        log.info("SUCCESS → Retrieved document summary for employee ID: {}", employeeId);

        return ResponseEntity.ok(summary);
    }

    //branch and department
    @GetMapping("/branch/{branchId}")
    public ResponseEntity<Map<String, Object>> getMonthlySummaryByBranch(
            @PathVariable Integer branchId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate) {

        log.info("API CALL → Get Monthly Summary By Branch | branchId={} startDate={} endDate={}",
                branchId, startDate, endDate);

        Map<String, Object> response = documentHeaderService.getMonthlyApprovalSummary("branch", branchId, startDate, endDate);

        log.info("SUCCESS → Retrieved monthly summary for branch ID: {}", branchId);

        return ResponseEntity.ok(response);
    }


    @GetMapping("/department/{departmentId}")
    public ResponseEntity<Map<String, Object>> getMonthlySummaryByDepartment(
            @PathVariable Integer departmentId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate) {

        log.info("API CALL → Get Monthly Summary By Department | departmentId={} startDate={} endDate={}",
                departmentId, startDate, endDate);

        Map<String, Object> response = documentHeaderService.getMonthlyApprovalSummary("department", departmentId, startDate, endDate);

        log.info("SUCCESS → Retrieved monthly summary for department ID: {}", departmentId);

        return ResponseEntity.ok(response);
    }

    //system admin

    @GetMapping("/monthly-total")
    public ResponseEntity<Map<String, Object>> getMonthlyTotalSummary(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate) {

        log.info("API CALL → Get Monthly Total Summary | startDate={} endDate={}", startDate, endDate);

        Map<String, Object> response = documentHeaderService.getTotalMonthlySummary(startDate, endDate);

        log.info("SUCCESS → Retrieved monthly total summary");

        return ResponseEntity.ok(response);
    }


    @GetMapping("/top-branches-summary")
    public ResponseEntity<Map<String, Object>> getTopBranchSummary(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate) {

        log.info("API CALL → Get Top Branches Summary | startDate={} endDate={}", startDate, endDate);

        Map<String, Object> response = documentHeaderService.getTotalSummaryByTopBranches(startDate, endDate);

        log.info("SUCCESS → Retrieved top branches summary");

        return ResponseEntity.ok(response);
    }




    @PostMapping("/export")
    public ResponseEntity<Void> exportDocuments(@RequestBody DocFilterRequest filterRequest, HttpServletResponse response) throws Exception {
        log.info("API CALL → Export Documents | docType={} branchId={} departmentId={} categoryId={}",
                filterRequest.getDocType(), filterRequest.getBranchId(), filterRequest.getDepartmentId(), filterRequest.getCategoryId());

        String contentType = "PDF".equalsIgnoreCase(filterRequest.getDocType()) ? "application/pdf" : "application/vnd.ms-excel";
        String fileExtension = "PDF".equalsIgnoreCase(filterRequest.getDocType()) ? "pdf" : "xlsx";

        String branchName = branchMasterRepository.findNameById(filterRequest.getBranchId()).orElse("AllBranch");
        String departmentName = departmentMasterRepository.findNameById(filterRequest.getDepartmentId()).orElse("AllDepartment");
        String categoryName = categoryMasterRepository.findNameById(filterRequest.getCategoryId()).orElse("AllCategory");

        String fileName;
        if ("PDF".equalsIgnoreCase(filterRequest.getDocType())) {
            fileName = String.format("%s_%s_%s.pdf", branchName, departmentName, categoryName);
        } else {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

            String fromDate = filterRequest.getStartDate() != null
                    ? filterRequest.getStartDate().toLocalDateTime().toLocalDate().format(formatter)
                    : "Start";

            String toDate = filterRequest.getEndDate() != null
                    ? filterRequest.getEndDate().toLocalDateTime().toLocalDate().format(formatter)
                    : "End";

            String approvalStatus = filterRequest.getApprovalStatus() != null
                    ? filterRequest.getApprovalStatus().toString()
                    : "AllStatus";

            fileName = String.format("%s_%s_%s_%s_%s_To_%s.xlsx",
                    branchName, departmentName, categoryName, approvalStatus, fromDate, toDate);
        }

        response.setContentType(contentType);
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");

        log.debug("Exporting documents with fileName: {}", fileName);
        documentHeaderService.exportDocuments(response.getOutputStream(), filterRequest);

        log.info("SUCCESS → Documents exported | fileName={}", fileName);

        return ResponseEntity.ok()
                .header("Exported-File-Name", fileName)
                .build();
    }

    @PostMapping("/export/ById")
    public ResponseEntity<Void> exportDocumentsById(@RequestBody DocFilterRequest filterRequest, HttpServletResponse response) throws Exception {
        log.info("API CALL → Export Documents By ID | docType={} branchId={} departmentId={} categoryId={}",
                filterRequest.getDocType(), filterRequest.getBranchId(), filterRequest.getDepartmentId(), filterRequest.getCategoryId());

        // Determine content type and file extension based on request
        String contentType = "PDF".equalsIgnoreCase(filterRequest.getDocType()) ? "application/pdf" : "application/vnd.ms-excel";
        String fileExtension = "PDF".equalsIgnoreCase(filterRequest.getDocType()) ? "pdf" : "xlsx";

        String branchName = branchMasterRepository.findNameById(filterRequest.getBranchId()).orElse("AllBranch");
        String departmentName = departmentMasterRepository.findNameById(filterRequest.getDepartmentId()).orElse("AllDepartment");
        String categoryName = categoryMasterRepository.findNameById(filterRequest.getCategoryId()).orElse("AllCategory");

        String fileName;
        if ("PDF".equalsIgnoreCase(filterRequest.getDocType())) {
            fileName = String.format("%s_%s_%s.pdf", branchName, departmentName, categoryName);
        } else {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

            String fromDate = filterRequest.getStartDate() != null
                    ? filterRequest.getStartDate().toLocalDateTime().toLocalDate().format(formatter)
                    : "Start";

            String toDate = filterRequest.getEndDate() != null
                    ? filterRequest.getEndDate().toLocalDateTime().toLocalDate().format(formatter)
                    : "End";

            String approvalStatus = filterRequest.getApprovalStatus() != null
                    ? filterRequest.getApprovalStatus().toString()
                    : "AllStatus";

            fileName = String.format("%s_%s_%s_%s_%s_To_%s.xlsx",
                    branchName, departmentName, categoryName, approvalStatus, fromDate, toDate);
        }

        response.setContentType(contentType);
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");

        log.debug("Exporting documents by ID with fileName: {}", fileName);
        documentHeaderService.exportDocumentsById(response.getOutputStream(), filterRequest);

        log.info("SUCCESS → Documents exported by ID | fileName={}", fileName);

        return ResponseEntity.ok()
                .header("Exported-File-Name", fileName)
                .build();
    }

    //============================QR Codes=======================================
    // View QR Code details


    @GetMapping("/documents/download/qr/{documentId}")
    public ResponseEntity<byte[]> downloadQRCode(@PathVariable Integer documentId) throws IOException {

        log.info("API CALL → Download QR Code | documentId={}", documentId);

        DocumentHeader documentHeader = documentHeaderRepository.findById(documentId)
                .orElseThrow(() -> {
                    log.error("FAILED → Download QR Code | documentId={} reason=Document Not Found", documentId);
                    return new ResourceNotFoundException("Document not found with id " + documentId);
                });

        String qrPath = documentHeader.getQrPath();
        if (qrPath == null || qrPath.isEmpty()) {
            log.error("FAILED → Download QR Code | documentId={} reason=QR Path Not Found", documentId);
            throw new ResourceNotFoundException("QR Code not found for document with id " + documentId);
        }

        Path qrCodeFilePath = Paths.get(documentStoragePath, qrPath);

        if (!Files.exists(qrCodeFilePath)) {
            log.error("FAILED → Download QR Code | documentId={} reason=QR File Not Found | path={}",
                    documentId, qrCodeFilePath.toString());
            throw new ResourceNotFoundException("QR Code file not found at path: " + qrCodeFilePath.toString());
        }

        byte[] qrCodeBytes = Files.readAllBytes(qrCodeFilePath);

        log.info("SUCCESS → QR Code downloaded | documentId={}", documentId);

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + qrCodeFilePath.getFileName()) // Set filename in the response header
                .body(qrCodeBytes);
    }


    //read
    @PostMapping("/read")
    public ResponseEntity<Map<String, String>> readQRCode(@RequestParam("file") MultipartFile file) {
        log.info("API CALL → Read QR Code | fileName={} fileSize={}",
                file.getOriginalFilename(), file.getSize());

        Path tempFile = null;

        try {
            if (file.isEmpty()) {
                log.error("FAILED → Read QR Code | reason=Empty File");
                throw new IllegalArgumentException("Uploaded file is empty.");
            }

            tempFile = Files.createTempFile("uploaded-qr", ".png");
            Files.write(tempFile, file.getBytes());

            String qrContent = qrCodeGenerator.readQRCode(tempFile.toString());

            Map<String, String> response = new HashMap<>();
            response.put("qrContent", qrContent);

            log.info("SUCCESS → QR Code read successfully | contentLength={}", qrContent.length());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("FAILED → Read QR Code | reason={}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("ERROR → Read QR Code | reason={}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    log.warn("Failed to delete temp file | reason={}", e.getMessage());
                }
            }
        }
    }


    @GetMapping("/findByDocName/{docName}")
    public ResponseEntity<ApiResponse<DocumentHeader>> findProjectByDocName(@PathVariable String docName) {
        log.info("API CALL → Find Project By Document Name | docName={}", docName);

        ApiResponse<DocumentHeader> response = documentHeaderService.findProjectByDocName(docName);

        HttpStatus status = response.getStatus() == HttpStatus.OK.value() ? HttpStatus.OK : HttpStatus.NOT_FOUND;

        if (status == HttpStatus.OK) {
            log.info("SUCCESS → Project found by document name | docName={}", docName);
        } else {
            log.warn("Project not found by document name | docName={}", docName);
        }

        return new ResponseEntity<>(response, status);
    }



    @GetMapping("/top-file-types")
    public List<FileTypeCountDTO> getTopFileTypesYearWise() {
        log.info("API CALL → Get Top File Types Year Wise");

        List<FileTypeCountDTO> result = documentDetailsService.getTop10FileTypesByYear();

        log.info("SUCCESS → Retrieved top file types for {} years", result.size());

        return result;
    }

    @GetMapping("/getFile/{fileNo}")
    public ResponseEntity<DocumentResponse2> getDocuments(@PathVariable String fileNo) {
        log.info("API CALL → Get Documents By File No | fileNo={}", fileNo);

        DocumentResponse2 response = documentHeaderService.getDocumentsByFileNo(fileNo);

        log.info("SUCCESS → Retrieved documents for fileNo: {}", fileNo);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/getAllDocument")
    public ResponseEntity<List<DocumentHeader>> getAllDocumentHeaders(@AuthenticationPrincipal Employee currentEmployee) {
        log.info("API CALL → Get All Filtered Documents | currentEmployeeId={}",
                currentEmployee != null ? currentEmployee.getId() : "null");

        List<DocumentHeader> documentHeaders = documentHeaderService.findAllFilterDocumentHeaders();

        log.info("SUCCESS → Retrieved {} filtered documents", documentHeaders.size());

        return new ResponseEntity<>(documentHeaders, HttpStatus.OK);
    }


    @PostMapping("/compare")
    public ResponseEntity<ApiResponse<FileCompareResponse>> compareFiles(@RequestBody FileCompareRequest request) {
        log.info("API CALL → Compare Files | firstFileId={} secondFileId={}",
                request.getFirstFileId(), request.getSecondFileId());

        logger.info("Comparing files with IDs: {} vs {}", request.getFirstFileId(), request.getSecondFileId());

        ApiResponse<FileCompareResponse> response = documentDetailsService.compareFiles(request);

        log.info("SUCCESS → Files comparison completed | firstFileId={} secondFileId={}",
                request.getFirstFileId(), request.getSecondFileId());

        return ResponseEntity.status(response.getStatus()).body(response);
    }


    @GetMapping("/getAllDocumentsAuditLog")
    public ResponseEntity<List<DocumentsAuditLog>> getAllDocumentsAuditLog() {
        log.info("API CALL → Get All Documents Audit Log");

        List<DocumentsAuditLog> documentsAuditLog = documentsAuditLogService.findAllDocumentsAuditLog();

        log.info("SUCCESS → Retrieved {} documents audit logs", documentsAuditLog.size());

        return new ResponseEntity<>(documentsAuditLog, HttpStatus.OK);
    }

    @PostMapping("/createDocumentsAuditLog")
    public ResponseEntity<ApiResponse<DocumentsAuditLog>> compareFiles(@RequestBody DocumentsAuditLogRequest  request) {
        log.info("API CALL → Create Documents Audit Log");

        ApiResponse<DocumentsAuditLog> response = documentsAuditLogService.createLog(request);

        log.info("SUCCESS → Documents audit log created | status={}", response.getStatus());

        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @GetMapping("/dateRangeBetween/{start}/{end}")
    public ResponseEntity<ApiResponse<List<DocumentsAuditLog>>> getLogsBetween(
            @PathVariable("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @PathVariable("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {

        log.info("API CALL → Get Logs Between Dates | start={} end={}", start, end);

        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.plusDays(1).atStartOfDay().minusNanos(1);

        ApiResponse<List<DocumentsAuditLog>> response = documentsAuditLogService.getLogsBetweenDates(startDateTime, endDateTime);

        log.info("SUCCESS → Retrieved {} audit logs between dates",
                response.getResponse() != null ? response.getResponse().size() : 0);

        return ResponseEntity.ok(response);
    }


    @PutMapping("/delete-status/{id}")
    public ResponseEntity<DocumentDetails> updateDeleteStatus(
            @PathVariable Integer id,
            @RequestParam Boolean isDeleted,
            HttpServletRequest request) {

        log.info("API CALL → Update Delete Status | id={} isDeleted={}", id, isDeleted);

        DocumentDetails result = documentDetailsService.updateDeleteStatus(id, isDeleted, request);

        log.info("SUCCESS → Delete status updated | id={} isDeleted={}", id, isDeleted);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/approvedTrashByEmp")
    public ResponseEntity<List<DocumentHeader>> getApprovedTrashByEmployee(
            @RequestHeader("employeeId") Integer employeeId) {

        log.info("API CALL → Get Approved Trash By Employee | employeeId={}", employeeId);

        List<DocumentHeader> trashDocuments =
                documentHeaderService.findAllTrashApprovedByEmployeeId(employeeId);

        log.info("SUCCESS → Retrieved {} approved trash documents for employee ID: {}",
                trashDocuments.size(), employeeId);

        return ResponseEntity.ok(trashDocuments);
    }





    // GET: Get all duplicate documents
    @GetMapping("/duplicates")
    public ResponseEntity<ApiResponse<List<DuplicateDocumentResponse>>> getDuplicateDocuments() {
        log.info("API CALL → Get Duplicate Documents");

        List<DuplicateDocumentResponse> duplicates = documentHeaderService.getDuplicateDocuments();

        ApiResponse<List<DuplicateDocumentResponse>> response = new ApiResponse<>();
        response.setStatus(HttpStatus.OK.value());
        response.setMessage("Duplicate documents retrieved successfully");
        response.setResponse(duplicates);

        log.info("SUCCESS → Retrieved {} duplicate documents", duplicates.size());

        return ResponseEntity.ok(response);
    }

    // DELETE: Delete a single duplicate file
    @DeleteMapping("/duplicates/{duplicateId}")
    public ResponseEntity<ApiResponse<MessageResponse>> deleteDuplicateFile(
            @PathVariable Integer duplicateId,
            HttpServletRequest request) {

        log.info("API CALL → Delete Duplicate File | duplicateId={}", duplicateId);

        ApiResponse<MessageResponse> response = documentHeaderService.deleteDuplicateFile(duplicateId, request);

        log.info("SUCCESS → Duplicate file deleted | duplicateId={}", duplicateId);

        return ResponseEntity.status(response.getStatus()).body(response);
    }

    // DELETE: Delete all duplicates for an original document
    @DeleteMapping("/duplicates/original/{originalId}")
    public ResponseEntity<ApiResponse<MessageResponse>> deleteAllDuplicatesForOriginal(
            @PathVariable Integer originalId,
            HttpServletRequest request) {

        log.info("API CALL → Delete All Duplicates For Original | originalId={}", originalId);

        ApiResponse<MessageResponse> response = documentHeaderService.deleteAllDuplicatesForOriginal(originalId, request);

        log.info("SUCCESS → All duplicates deleted for original document | originalId={}", originalId);

        return ResponseEntity.status(response.getStatus()).body(response);
    }


    @GetMapping("/findByDetailsId/{detailsId}")
    public ResponseEntity<ApiResponse<DocumentHeader>> findDocumentHeaderByDetailsId(@PathVariable Integer detailsId) {
        log.info("API CALL → Find Document Header By Details ID | detailsId={}", detailsId);

        Optional<DocumentDetails> documentDetails = documentDetailsRepository.findById(detailsId);

        if (documentDetails.isPresent()) {
            DocumentHeader documentHeader = documentDetails.get().getDocumentHeader();
            log.info("SUCCESS → Found document header for details ID: {}", detailsId);
            ApiResponse<DocumentHeader> response = ResponseUtils.createSuccessResponse(
                    documentHeader,
                    new TypeReference<DocumentHeader>() {}
            );
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            log.warn("Document details not found with ID: {}", detailsId);
            ApiResponse<DocumentHeader> response = ResponseUtils.createNotFoundResponse(
                    "Document details not found with ID: " + detailsId,
                    HttpStatus.NOT_FOUND.value()
            );
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
    }



    @GetMapping("/next-version/{headerId}/{yearId}")
    public ResponseEntity<ApiResponse<Map<String, String>>> getNextVersion(
            @PathVariable Integer headerId,
            @PathVariable Integer yearId,
            @RequestParam(defaultValue = "patch") String changeType) {

        log.info("API CALL → Get Next Version | headerId={} yearId={} changeType={}",
                headerId, yearId, changeType);

        try {
            // ✅ Use enhanced method with change type
            String nextVersion = documentDetailsService.getNextVersionWithChangeType(
                    headerId, yearId, changeType
            );

            Map<String, String> response = new HashMap<>();
            response.put("nextVersion", nextVersion);
            response.put("changeType", changeType);

            log.info("SUCCESS → Next version generated: {}", nextVersion);

            return ResponseEntity.ok(
                    ResponseUtils.createSuccessResponse(response, new TypeReference<>() {})
            );
        } catch (Exception e) {
            log.error("FAILED → Get Next Version | reason={}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseUtils.createFailureResponse(
                            null, new TypeReference<>() {},
                            "Failed to generate next version: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()
                    ));
        }
    }
    //Get version history
    @GetMapping("/version-history/{headerId}/{yearId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getVersionHistory(
            @PathVariable Integer headerId,
            @PathVariable Integer yearId) {

        log.info("API CALL → Get Version History | headerId={} yearId={}", headerId, yearId);

        try {
            Map<String, Object> history = documentDetailsService.getVersionHistory(headerId, yearId);

            log.info("SUCCESS → Retrieved version history: {} versions",
                    history.get("totalVersions"));

            return ResponseEntity.ok(
                    ResponseUtils.createSuccessResponse(history, new TypeReference<>() {})
            );
        } catch (Exception e) {
            log.error("FAILED → Get Version History | reason={}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseUtils.createFailureResponse(
                            null, new TypeReference<>() {},
                            "Failed to get version history: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()
                    ));
        }
    }
}