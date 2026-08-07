package com.dmsBackend.service;

import com.dmsBackend.entity.DocumentDetails;
import com.dmsBackend.entity.DocumentHeader;
import com.dmsBackend.entity.Employee;
import com.dmsBackend.response.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface DocumentShareService {

    ApiResponse<DocumentShareResponse> shareDocument(DocumentShareRequest request, HttpServletRequest httpRequest);

    // Add the bulk share method that's missing
    ApiResponse<DocumentShareResponse> bulkShareDocuments(BulkShareRequest request, HttpServletRequest httpRequest);

    List<DocumentDetails> getSharedDocumentsForCurrentUser();

    List<DocumentShareResponse.ShareInfoResponse> getDocumentsSharedByCurrentUser();

    List<DocumentShareResponse.ShareInfoResponse> getSharesForDocument(Integer documentHeaderId);

    ApiResponse<MessageResponse> revokeShare(ShareRevokeRequest request, HttpServletRequest httpRequest);

    boolean isDocumentSharedWithUser(Integer documentId);

    List<Employee> getSameDepartmentEmployees();

    void cleanupExpiredShares();

    List<DocumentHeader> getSharedDocumentHeadersForCurrentUser();

    // Add these missing methods that are used in the implementation
    boolean hasDocumentShares(Integer documentHeaderId);

    int getShareCountForDocument(Integer documentHeaderId);

    List<DocumentShareResponse.ShareInfoResponse> getDocumentsSharedWithCurrentUser();
}