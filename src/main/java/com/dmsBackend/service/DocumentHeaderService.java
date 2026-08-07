package com.dmsBackend.service;

import com.dmsBackend.entity.DocumentDetails;
import com.dmsBackend.entity.DocumentHeader;
import com.dmsBackend.entity.DocApprovalStatus;
import com.dmsBackend.entity.Employee;
import com.dmsBackend.response.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface DocumentHeaderService {

//    DocumentHeader saveDocumentHeader(DocumentHeader documentHeader);

//    DocumentHeader updateDocumentHeader(Integer id, DocumentHeader updatedDocument);

    DocumentHeader findDocumentHeaderById(Integer id);

    List<DocumentHeader> findDocumentHeaderByBranchId(Integer id);

    List<DocumentHeader> findDocumentHeaderByDepartmentId(Integer id);

    List<DocumentHeader> findAllDocumentHeaders();

    public List<DocumentHeader> findAllFilterDocumentHeaders();

    void deleteByIdDocumentHeader(Integer id);

    DocumentHeader updateApprovalStatus(Integer id, DocApprovalStatus status, String rejectionReason, Integer employeeId);

    ApiResponse<Map<String, Object>> saveDocumentWithFiles(DocumentSaveRequest documentSaveRequest, HttpServletRequest request);

    //update documents


    @Transactional
    ApiResponse<MessageResponse> updateDocumentWithFiles(
            DocumentHeader documentHeader,
            List<DocumentSaveRequest.MetadataRequest> metadata,
            List<Long> deletedMetaDataIds,
            List<DocumentSaveRequest.FilePathVersion> filePaths,
            String version,
            HttpServletRequest request);

    DocumentHeader updateActiveStatus(Integer id, boolean isActive);

    List<DocumentHeader> getAllApproved();

    List<DocumentHeader> getAllRejected();

    List<DocumentHeader> getAllPending();

    List<DocumentHeader> getAllApprovedByEmployeeId(Integer employeeId);

    List<DocumentHeader> getAllRejectedByEmployeeId(Integer employeeId);

    List<DocumentHeader> getAllPendingByEmployeeId(Integer employeeId);

    List<DocumentHeader> findAllDocumentHeadersByEmployeeId(Integer employeeId);

    //  Admin
    List<DocumentHeader> findAllRejectedByActionEmployeeId(Integer employeeId);

    List<DocumentHeader> findAllApprovedByActionEmployeeId(Integer employeeId);

     List<DocumentHeader> findAllTrashApprovedByEmployeeId(Integer employeeId);

    //For Count

    long countApprovedDocuments();

    long countRejectedDocuments();

    long countPendingDocuments();

    long countApprovedDocumentsByEmployeeId(Integer employeeId);

    long countRejectedDocumentsByEmployeeId(Integer employeeId);

    long countPendingDocumentsByEmployeeId(Integer employeeId);

    long countDocumentHeadersByEmployeeId(Integer employeeId);

    long countRejectedByActionEmployeeId(Integer employeeId);

    long countApprovedByActionEmployeeId(Integer employeeId);

    //For Graph
    Map<String, Object> countAllDocumentsByIdWithMonth(Integer employeeId, Timestamp startDate, Timestamp endDate);

    Map<String, Object> getApprovalSummaryByEmployeeId(Integer employeeId, LocalDateTime startDate, LocalDateTime endDate);


    public List<DocumentHeader> getPendingDocumentsByBranch(Integer branchId);

    //List<DocumentHeader> getApprovedDocumentsByBranchAdmin(Integer userId);

    public List<DocumentHeader> findApprovedDocumentsByBranchAdmin(Integer employeeId);

    List<DocumentHeader> getAllPendingDocuments();

    // List<DocumentHeader> getPendingDocumentsForAllBranches();


//    DocumentHeader getDocumentHeaderById(Integer headerId);

    Long countDocumentHeadersByBranchId(Integer branch);
    Long countPendingDocumentsByBranchId(Integer branch);
    Long countApprovedByBranchId(Integer branch);
    Long countRejectedByBranchId(Integer branch);

    List<DocumentHeader> searchDocuments(SearchCriteria criteria);


    List<DocumentHeader> getPendingDocumentsByDepartment(Integer departmentId);

    List<DocumentResponse> getFilteredDocuments(Integer categoryId, DocApprovalStatus approvalStatus,
                                                Timestamp startDate, Timestamp endDate,
                                                Integer branchId, Integer departmentId);


//    void exportDocuments(DocFilterRequest request, HttpServletResponse response) throws IOException;

    List<DocumentResponse> getFilteredDocumentsById(Integer categoryId, DocApprovalStatus approvalStatus,
                                                    Timestamp startDate, Timestamp endDate,
                                                    Integer branchId, Integer departmentId, Integer employeeId);

    long countDocumentHeadersByDepartmentId(Integer departmentId);

    long countPendingDocumentsByDepartmentId(Integer departmentId);

    long countApprovedByDepartmentId(Integer departmentId);

    long countRejectedByDepartmentId(Integer departmentId);


    Map<String, Object> getMonthlyApprovalSummary(String queryType, Integer departmentOrBranchId, LocalDateTime startDate, LocalDateTime endDate);


    Map<String, Object> getTotalMonthlySummary(LocalDateTime startDate, LocalDateTime endDate);

    Map<String, Object> getTotalSummaryByTopBranches(LocalDateTime startDate, LocalDateTime endDate);

    void exportDocuments(OutputStream outputStream, DocFilterRequest filterRequest) throws Exception;

    ApiResponse<DocumentHeader> findProjectByDocName(String docName);
    void exportDocumentsById(OutputStream outputStream, DocFilterRequest filterRequest) throws Exception;

    DocumentResponse2 getDocumentsByFileNo(String fileNo);

    List<DuplicateDocumentResponse> getDuplicateDocuments();

    ApiResponse<MessageResponse> deleteDuplicateFile(Integer duplicateId, HttpServletRequest request);

    ApiResponse<MessageResponse> deleteAllDuplicatesForOriginal(Integer originalId, HttpServletRequest request);
}
