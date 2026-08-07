package com.dmsBackend.service;

import com.dmsBackend.entity.*;
import com.dmsBackend.response.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface DocumentDetailsService {


//    List<String> uploadFiles(List<MultipartFile> files, String branch, String department, String category, String year, String version);


    @Transactional
    Map<String, Object> uploadFiles(List<MultipartFile> files,
                                    String branch, String department, String category,
                                    String year, String version,
                                    List<Integer> waitingRoomIds); // ✅ Add parameter
//    void saveFileDetails(DocumentHeader documentHeader, List<DocumentSaveRequest.FilePathVersion> filePaths);

    void saveFileDetails(DocumentHeader header, List<DocumentSaveRequest.FilePathVersion> files, String actor);

    @Transactional
    void updateDetailStatus(Integer detailId, DocApprovalStatus newStatus, String reason, HttpServletRequest request);

    @Transactional
    List<DocumentDetails> updateFileDetails(
            CategoryMaster categoryMaster,
            YearMaster yearMaster,
            DocumentHeader documentHeader,
            List<DocumentSaveRequest.FilePathVersion> filePaths,
            String version,
            boolean updatePaths);

    List<DocumentDetailsResponse> findDocumentsByHeaderId(Integer headerId, DocApprovalStatus status);
    List<DocumentDetailsResponse> findDocumentsByHeaderId(Integer headerId);

    List<FileTypeCountDTO> getTop10FileTypesByYear();

    ApiResponse<FileCompareResponse> compareFiles(FileCompareRequest request);

    List<Integer> saveFileDetailsWithWaitingRoom(DocumentHeader header,
                                                 List<DocumentSaveRequest.FilePathVersion> filePaths,
                                                 String userEmail);



    public DocumentDetails updateDeleteStatus(Integer detailId, Boolean isDeleted, HttpServletRequest request);


    Long countTotalDocByBranchId(Integer branch);

    // counts new
    Long countApprovedByBranchId(Integer branch);

    Long countPendingDocumentsByBranchId(Integer branch);

    Long countRejectedByBranchId(Integer branch);

    Long countTotalDocByDepartmentId(Integer departmentId);

    Long countPendingDocumentsByDepartmentId(Integer departmentId);

    Long countApprovedDetailsByDepartmentId(Integer departmentId);

    Long countRejectedByDepartmentId(Integer departmentId);

    Long countApprovedDetails(DocApprovalStatus status);

    //Generate next version number
    String generateNextVersion(Integer headerId, Integer yearId);

    String getNextVersionWithChangeType(Integer headerId, Integer yearId, String changeType);

    //Get version history
    Map<String, Object> getVersionHistory(Integer headerId, Integer yearId);
}
