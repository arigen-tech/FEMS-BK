package com.dmsBackend.controller;

import com.dmsBackend.entity.DocumentHeader;
import com.dmsBackend.entity.Employee;
import com.dmsBackend.response.ApiResponse;
import com.dmsBackend.response.DocumentShareRequest;
import com.dmsBackend.response.BulkShareRequest;
import com.dmsBackend.response.DocumentShareResponse;
import com.dmsBackend.response.ShareRevokeRequest;
import com.dmsBackend.response.MessageResponse;
import com.dmsBackend.service.DocumentShareService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/document-share")
@CrossOrigin(origins = "*")
@Slf4j
public class DocumentShareController {

    @Autowired
    private DocumentShareService documentShareService;

    /**
     * Share a document with employees in the same department
     */
    @PostMapping("/share")
    public ResponseEntity<ApiResponse<DocumentShareResponse>> shareDocument(
            @RequestBody DocumentShareRequest request,
            HttpServletRequest httpRequest) {

        log.info("API CALL → Share Document | documentId={}", request.getDocumentHeaderId());

        ApiResponse<DocumentShareResponse> response =
                documentShareService.shareDocument(request, httpRequest);

        log.info("API RESPONSE → Share Document | status={}", response.getStatus());

        return ResponseEntity.status(response.getStatus()).body(response);
    }

    /**
     * Bulk share multiple documents with employees in the same department
     */
    @PostMapping("/bulk-share")
    public ResponseEntity<ApiResponse<DocumentShareResponse>> bulkShareDocuments(
            @RequestBody BulkShareRequest request,
            HttpServletRequest httpRequest) {

        log.info("API CALL → Bulk Share Documents | documentCount={}",
                request.getDocumentHeaderIds() != null ? request.getDocumentHeaderIds().size() : 0);

        ApiResponse<DocumentShareResponse> response =
                documentShareService.bulkShareDocuments(request, httpRequest);

        log.info("API RESPONSE → Bulk Share | status={}", response.getStatus());

        return ResponseEntity.status(response.getStatus()).body(response);
    }

    /**
     * Get all documents shared with current user
     */
    @GetMapping("/shared-with-me")
    public ResponseEntity<List<DocumentShareResponse.ShareInfoResponse>> getSharedDocuments() {

        log.info("API CALL → Get Documents Shared With Me");

        List<DocumentShareResponse.ShareInfoResponse> sharedDocuments =
                documentShareService.getDocumentsSharedWithCurrentUser();

        log.info("API RESPONSE → Shared With Me | count={}", sharedDocuments.size());

        return ResponseEntity.ok(sharedDocuments);
    }

    /**
     * Get all documents shared by current user
     */
    @GetMapping("/shared-by-me")
    public ResponseEntity<List<DocumentShareResponse.ShareInfoResponse>> getDocumentsSharedByMe() {

        log.info("API CALL → Get Documents Shared By Me");

        List<DocumentShareResponse.ShareInfoResponse> sharedDocuments =
                documentShareService.getDocumentsSharedByCurrentUser();

        log.info("API RESPONSE → Shared By Me | count={}", sharedDocuments.size());

        return ResponseEntity.ok(sharedDocuments);
    }

    /**
     * Get shares for a specific document
     */
    @GetMapping("/document/{documentHeaderId}/shares")
    public ResponseEntity<List<DocumentShareResponse.ShareInfoResponse>> getDocumentShares(
            @PathVariable Integer documentHeaderId) {

        log.info("API CALL → Get Document Shares | documentHeaderId={}", documentHeaderId);

        List<DocumentShareResponse.ShareInfoResponse> shares =
                documentShareService.getSharesForDocument(documentHeaderId);

        log.info("API RESPONSE → Document Shares | documentHeaderId={} count={}",
                documentHeaderId, shares.size());

        return ResponseEntity.ok(shares);
    }

    /**
     * Revoke a document share
     */
    @PostMapping("/revoke")
    public ResponseEntity<ApiResponse<MessageResponse>> revokeShare(
            @RequestBody ShareRevokeRequest request,
            HttpServletRequest httpRequest) {

        log.info("API CALL → Revoke Share | documentId={} employeeId={}",
                request.getShareId(), request.getShareId());

        ApiResponse<MessageResponse> response =
                documentShareService.revokeShare(request, httpRequest);

        log.info("API RESPONSE → Revoke Share | status={}", response.getStatus());

        return ResponseEntity.status(response.getStatus()).body(response);
    }

    /**
     * Check if document is shared with current user
     */
    @GetMapping("/check/{documentId}")
    public ResponseEntity<Boolean> checkDocumentShare(@PathVariable Integer documentId) {

        log.info("API CALL → Check Document Share | documentId={}", documentId);

        boolean isShared =
                documentShareService.isDocumentSharedWithUser(documentId);

        log.info("API RESPONSE → Check Document Share | documentId={} shared={}",
                documentId, isShared);

        return ResponseEntity.ok(isShared);
    }

    /**
     * Get employees in same department for sharing
     */
    @GetMapping("/same-department-employees")
    public ResponseEntity<List<Employee>> getSameDepartmentEmployees() {

        log.info("API CALL → Get Same Department Employees");

        List<Employee> employees =
                documentShareService.getSameDepartmentEmployees();

        log.info("API RESPONSE → Same Department Employees | count={}", employees.size());

        return ResponseEntity.ok(employees);
    }

    /**
     * Get shared document headers for current user
     */
    @GetMapping("/shared-headers")
    public ResponseEntity<List<DocumentHeader>> getSharedDocumentHeaders() {

        log.info("API CALL → Get Shared Document Headers");

        List<DocumentHeader> sharedHeaders =
                documentShareService.getSharedDocumentHeadersForCurrentUser();

        log.info("API RESPONSE → Shared Headers | count={}", sharedHeaders.size());

        return ResponseEntity.ok(sharedHeaders);
    }

    /**
     * Check if a document has any shares
     */
    @GetMapping("/document/{documentHeaderId}/has-shares")
    public ResponseEntity<Boolean> hasDocumentShares(
            @PathVariable Integer documentHeaderId) {

        log.info("API CALL → Check Has Shares | documentHeaderId={}", documentHeaderId);

        boolean hasShares =
                documentShareService.hasDocumentShares(documentHeaderId);

        log.info("API RESPONSE → Has Shares | documentHeaderId={} result={}",
                documentHeaderId, hasShares);

        return ResponseEntity.ok(hasShares);
    }

    /**
     * Get share count for a document
     */
    @GetMapping("/document/{documentHeaderId}/share-count")
    public ResponseEntity<Integer> getShareCountForDocument(
            @PathVariable Integer documentHeaderId) {

        log.info("API CALL → Get Share Count | documentHeaderId={}", documentHeaderId);

        int shareCount =
                documentShareService.getShareCountForDocument(documentHeaderId);

        log.info("API RESPONSE → Share Count | documentHeaderId={} count={}",
                documentHeaderId, shareCount);

        return ResponseEntity.ok(shareCount);
    }

    /**
     * Clean up expired shares (admin / scheduled)
     */
    @PostMapping("/cleanup-expired")
    public ResponseEntity<String> cleanupExpiredShares() {

        log.info("API CALL → Cleanup Expired Shares");

        documentShareService.cleanupExpiredShares();

        log.info("SUCCESS → Expired Shares Cleanup Completed");

        return ResponseEntity.ok("Expired shares cleanup completed");
    }
}
