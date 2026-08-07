package com.dmsBackend.controller;

import com.dmsBackend.entity.*;
import com.dmsBackend.response.*;
import com.dmsBackend.service.DocumentServiceExtApi;
import com.dmsBackend.utils.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/external/documents")
@RequiredArgsConstructor
public class ExternalDocumentController {

    private final DocumentServiceExtApi documentService;
    private final CurrentUser currentUser;





    // ===============================
    //  SAVE NEW DOCUMENT
    // ===============================
    @PostMapping(
            value = "/save",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ExternalDocumentSaveResponse> saveDocument(
            @RequestPart("data") ExternalDocumentSaveRequest request,
            @RequestPart("files") List<MultipartFile> files) {

        System.out.println("request"+request);
        Employee employee = currentUser.getCurrentEmployeeOrThrow();
        BranchMaster branch = employee.getBranch();
        DepartmentMaster department = employee.getDepartment();

        request.setFiles(files);

        DocumentHeader savedHeader =
                documentService.saveExternalDocument(request, employee, branch, department);

        ExternalDocumentSaveResponse response = new ExternalDocumentSaveResponse();
        response.setDocumentHeaderId(savedHeader.getId());
        response.setDocumentDetailIds(
                savedHeader.getDocumentDetails()
                        .stream()
                        .map(DocumentDetails::getId)
                        .toList()
        );
        response.setStatus("SUCCESS");
        response.setMessage("Document saved and approved successfully via external API.");

        return ResponseEntity.ok(response);
    }


    // ===============================
    // ADD FILES TO HEADER
    // ===============================
    @PostMapping(
            value = "/{headerId}/add-files",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ExternalDocumentSaveResponse> addFilesToHeader(
            @PathVariable Integer headerId,
            @RequestPart("data") AddFilesRequest request,
            @RequestPart("files") List<MultipartFile> files) {

        Employee employee = currentUser.getCurrentEmployeeOrThrow();

        DocumentHeader updatedHeader =
                documentService.addFilesToHeader(headerId, request, files, employee);

        ExternalDocumentSaveResponse response = new ExternalDocumentSaveResponse();
        response.setDocumentHeaderId(updatedHeader.getId());
        response.setDocumentDetailIds(
                updatedHeader.getDocumentDetails()
                        .stream()
                        .map(DocumentDetails::getId)
                        .toList()
        );
        response.setStatus("SUCCESS");
        response.setMessage("Files added successfully to existing document header.");

        return ResponseEntity.ok(response);
    }


    // ===============================
    //  GET APPROVED DOCUMENTS
    // ===============================
    @GetMapping("/uploaded")
    public ResponseEntity<List<DocumentViewResponse>> getApprovedDocuments() {

        Employee employee = currentUser.getCurrentEmployeeOrThrow();
        BranchMaster branch = employee.getBranch();
        DepartmentMaster department = employee.getDepartment();

        return ResponseEntity.ok(
                documentService.getApprovedDocuments(employee, branch, department)
        );
    }


    // ===============================
    // SEARCH BY HEADER
    // ===============================

    @GetMapping("/{id}")
    public ResponseEntity<DocumentViewResponse> getDocumentById(@PathVariable Integer id) {

        Employee employee = currentUser.getCurrentEmployeeOrThrow();

        DocumentViewResponse response =
                documentService.getDocumentViewById(id, employee);

        return ResponseEntity.ok(response);
    }


    // ===============================
    //  SEARCH DYNAMIC ADVANCED
    // ===============================

    @PostMapping("/search")
    public ResponseEntity<Page<DocumentViewResponse>> searchDocuments(
            @RequestBody ExDocumentSearchRequest request) {

        Employee employee = currentUser.getCurrentEmployeeOrThrow();
        BranchMaster branch = employee.getBranch();
        DepartmentMaster department = employee.getDepartment();

        Page<DocumentViewResponse> documents =
                documentService.searchDocuments(request, branch, department);

        return ResponseEntity.ok(documents);
    }



    // ===============================
    // Advanced download by document IDs
    // ===============================

    @PostMapping("/download/by-ids")
    public ResponseEntity<?> downloadByIds(@RequestBody @Valid AdvancedDownloadByIdsRequest request) throws Exception {
        Employee employee = currentUser.getCurrentEmployeeOrThrow();

        if (request.getDownloadType().isFileIncluded() && !request.isConfirmFiles()) {
            return ResponseEntity.ok(documentService.preCheckFilesByIds(request, employee));
        }

        InputStreamResource resource = documentService.prepareDownloadByIds(request, employee);

        if (resource == null) {
            return ResponseEntity.status(409).body(
                    "Some files are present on cold storage with these header IDs " +
                            request.getDocumentIds() +
                            ". Send a request to '/restore' to transfer these files to hot storage and try downloading later."
            );
        }

        return buildResponse(resource, request.getDownloadType());
    }


    @PostMapping("/download/by-date")
    public ResponseEntity<?> downloadByDate(@RequestBody @Valid AdvancedDownloadByDateRequest request) throws IOException {
        Employee employee = currentUser.getCurrentEmployeeOrThrow();

        if (!request.isConfirmFiles() && request.getDownloadType().isFileIncluded()) {
            return ResponseEntity.ok(documentService.preCheckFilesByDate(request, employee));
        }

        InputStreamResource resource = documentService.prepareDownloadByDate(request, employee);

        if (resource == null) {
            return ResponseEntity.status(409).body(
                    String.format(
                            "Some files are present on cold storage between %s and %s. " +
                                    "Send a request to '/restore' to transfer these files to hot storage and try downloading later.",
                            request.getFromDate(), request.getToDate()
                    )
            );
        }

        return buildResponse(resource, request.getDownloadType());
    }


    private ResponseEntity<InputStreamResource> buildResponse(
            InputStreamResource resource, DownloadType type) {

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" +
                                documentService.getDownloadFilename(type) + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);

    }



    // ===============================
    // Restore Files From LTO
    // ===============================


        @PostMapping("/restore")
        public ResponseEntity<?> restore(@RequestBody @Valid ExternalRestoreRequest request) {

            Employee employee = currentUser.getCurrentEmployeeOrThrow();

            documentService.restoreDocuments(request, employee);

            return ResponseEntity.accepted().body(
                    "Restore request accepted. Files will be available shortly."
            );
        }




}
