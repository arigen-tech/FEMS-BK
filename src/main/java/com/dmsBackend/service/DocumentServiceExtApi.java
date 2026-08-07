package com.dmsBackend.service;

import com.dmsBackend.entity.BranchMaster;
import com.dmsBackend.entity.DepartmentMaster;
import com.dmsBackend.entity.DocumentHeader;
import com.dmsBackend.entity.Employee;
import com.dmsBackend.response.*;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface DocumentServiceExtApi {
    DocumentHeader saveExternalDocument(ExternalDocumentSaveRequest request,
                                        Employee employee,
                                        BranchMaster branch,
                                        DepartmentMaster department);


    // ===============================
    // 2️⃣ ADD FILES TO HEADER
    // ===============================


    // ===============================
    // 2️⃣ ADD FILES TO HEADER
    // ===============================
    @Transactional
    DocumentHeader addFilesToHeader(
            Integer headerId,
            AddFilesRequest request,
            List<MultipartFile> files,
            Employee employee);

    List<DocumentViewResponse> getApprovedDocuments(Employee employee, BranchMaster branch, DepartmentMaster department);


    // ===============================
    // GET BY ID
    // ===============================
    @Transactional(readOnly = true)
    DocumentViewResponse getDocumentViewById(Integer id, Employee employee);


    // download


    // ===============================
    // ADVANCED SEARCH
    // ===============================
    @Transactional(readOnly = true)
    Page<DocumentViewResponse> searchDocuments(
            ExDocumentSearchRequest request,
            BranchMaster branch,
            DepartmentMaster department);

    // ===============================
    // ADVANCED DOWNLOAD
    // ===============================
    @Transactional(readOnly = true)
    AdvancedDownloadResponse preCheckFilesByIds(
            AdvancedDownloadByIdsRequest request, Employee employee);

    AdvancedDownloadResponse preCheckFilesByDate(
            AdvancedDownloadByDateRequest request, Employee employee);

    InputStreamResource prepareDownloadByIds(
            AdvancedDownloadByIdsRequest request, Employee employee) throws IOException;

    InputStreamResource prepareDownloadByDate(
            AdvancedDownloadByDateRequest request, Employee employee) throws IOException;

    String getDownloadFilename(DownloadType type);

    @Transactional
    void restoreDocuments(
            ExternalRestoreRequest request,
            Employee employee
    );
}
