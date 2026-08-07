package com.dmsBackend.service.Impl;


import com.dmsBackend.P5Archive.P5RestoreApiService;
import com.dmsBackend.entity.*;
import com.dmsBackend.repository.*;
import com.dmsBackend.response.*;
import com.dmsBackend.service.DocumentServiceExtApi;
import com.dmsBackend.utils.DocHelper;
import com.dmsBackend.utils.FileEncryptionUtil;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.CipherInputStream;
import java.io.*;
import java.nio.file.*;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.*;


@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceExtApiImpl implements DocumentServiceExtApi {

    private final DocumentHeaderRepository headerRepository;
    private final DocumentDetailsRepository detailsRepository;
    private final CategoryMasterRepository categoryMasterRepository;
    private final YearMasterRepository yearMasterRepository;

    private final P5RestoreApiService p5RestoreApiService;
    private final FileEncryptionUtil fileEncryptionUtil;

    private final FilesTypeMasterRepository filesTypeMasterRepository;
    private final ObjectMapper objectMapper;

    @Value("${document.storage.path}")
    private String basePath;

    @Value("${file.max.filesize}")
    private long filesizeUploads;

    private static final DecimalFormat df = new DecimalFormat("#.##");



    // ===============================
    // 1️⃣ SAVE DOCUMENT
    // ===============================
    @Transactional
    @Override
    public DocumentHeader saveExternalDocument(
            ExternalDocumentSaveRequest request,
            Employee employee,
            BranchMaster branch,
            DepartmentMaster department) {

        if (headerRepository.existsByFileNo(request.getFileNo())) {
            throw new RuntimeException("File number already exists: " + request.getFileNo());
        }

        Map<String, Object> uploadResult =
                uploadFilesUnique(
                        request.getFiles(),
                        branch.getName(),
                        department.getName(),
                        categoryMasterRepository.getById(request.getCategoryId()).getName(),
                        request.getYear(),
                        request.getVersion(),
                        request.getFileNo()
                );

        List<Map<String, Object>> uploadedFiles =
                (List<Map<String, Object>>) uploadResult.get("uploadedFiles");

        List<Map<String, String>> errors =
                (List<Map<String, String>>) uploadResult.get("errors");

        if (uploadedFiles.isEmpty()) {
            throw new RuntimeException("No files uploaded successfully");
        }
        if (!errors.isEmpty()) {
            throw new RuntimeException("Some files failed to upload: " + errors);
        }

        DocumentHeader header = new DocumentHeader();
        header.setTitle(request.getTitle());
        header.setFileNo(request.getFileNo());
        header.setSubject(request.getSubject());
        header.setEmployee(employee);
        header.setBranchMaster(branch);
        header.setDepartmentMaster(department);
        header.setCategoryMaster(categoryMasterRepository.getById(request.getCategoryId()));
        header.setExternalApiFlag(true);

        header.setApprovalStatus(DocApprovalStatus.APPROVED);
        header.setIfApproved(true);
        header.setIfPending(false);
        header.setIfRejected(false);
        header.setActive(true);

        header.setCreatedBy(employee.getName());
        header.setCreatedOn(new Timestamp(System.currentTimeMillis()));
        header.setUpdatedBy(employee.getName());
        header.setUpdatedOn(new Timestamp(System.currentTimeMillis()));

        header = headerRepository.save(header);

        saveMetadata(header, request.getMetadata());

        YearMaster currYear = yearMasterRepository.findByName(request.getYear()).get();
        List<DocumentDetails> detailsList =
                createDocumentDetails(uploadedFiles, header, request, employee, currYear);

        header.setDocumentDetails(detailsList);
        return header;
    }

    // ===============================
    // 2️⃣ ADD FILES TO HEADER
    // ===============================
    @Transactional
    @Override
    public DocumentHeader addFilesToHeader(
            Integer headerId,
            AddFilesRequest request,
            List<MultipartFile> files,
            Employee employee) {

        DocumentHeader header = headerRepository.findById(headerId)
                .orElseThrow(() -> new RuntimeException("Document header not found"));

        if (header.getApprovalStatus() != DocApprovalStatus.APPROVED) {
            throw new RuntimeException("Cannot add files to non-approved document");
        }

        Map<String, Object> uploadResult =
                uploadFilesUnique(
                        files,
                        header.getBranchMaster().getName(),
                        header.getDepartmentMaster().getName(),
                        header.getCategoryMaster().getName(),
                        request.getYear(),
                        request.getVersion(),
                        header.getFileNo()
                );

        List<Map<String, Object>> uploadedFiles =
                (List<Map<String, Object>>) uploadResult.get("uploadedFiles");

        List<Map<String, String>> errors =
                (List<Map<String, String>>) uploadResult.get("errors");

        if (uploadedFiles.isEmpty()) {
            throw new RuntimeException("No files uploaded successfully");
        }
        if (!errors.isEmpty()) {
            throw new RuntimeException("Some files failed to upload: " + errors);
        }
        YearMaster currYear = yearMasterRepository.findByName(request.getYear()).get();
        List<DocumentDetails> newDetails =
                createDocumentDetails(uploadedFiles, header, request, employee,currYear);

        header.getDocumentDetails().addAll(newDetails);
        header.setUpdatedBy(employee.getName());
        header.setUpdatedOn(new Timestamp(System.currentTimeMillis()));

        return headerRepository.save(header);
    }



    // ===============================
    // HELPERS
    // ===============================
    private List<DocumentDetails> createDocumentDetails(
            List<Map<String, Object>> uploadedFiles,
            DocumentHeader header,
            Object request,
            Employee employee,
            YearMaster year) {

        String version =
                request instanceof ExternalDocumentSaveRequest r ? r.getVersion()
                        : ((AddFilesRequest) request).getVersion();

        List<DocumentDetails> detailsList = new ArrayList<>();

        for (Map<String, Object> file : uploadedFiles) {

            DocumentDetails detail = new DocumentDetails();
            detail.setDocumentHeader(header);
            detail.setDocName((String) file.get("storedName"));
            detail.setPath((String) file.get("path"));
            detail.setVersion(version);
            detail.setYearMaster(year);
            detail.setMimeType((String) file.get("mimeType"));
            detail.setFileType((String) file.get("fileType"));
            detail.setFileSizeBytes(String.valueOf(((Number) file.get("fileSizeBytes")).longValue()));
            detail.setFileSizeHuman((String) file.get("fileSizeHuman"));
            detail.setPageCounts((Integer) file.get("pageCount"));

            detail.setStatus(DocApprovalStatus.APPROVED);
            detail.setCreatedBy(employee.getName());
            detail.setCreatedOn(new Timestamp(System.currentTimeMillis()));
            detail.setApprovedBy(employee.getName());
            detail.setApprovedOn(new Timestamp(System.currentTimeMillis()));
            detail.setUpdatedBy(employee.getName());
            detail.setUpdatedOn(new Timestamp(System.currentTimeMillis()));

            detailsList.add(detailsRepository.save(detail));
        }
        return detailsList;
    }


    private void saveMetadata(
            DocumentHeader header,
            List<ExternalDocumentSaveRequest.MetadataRequest> metadata) {

        if (metadata == null || metadata.isEmpty()) return;

        List<DocumentMetadata> metaList = new ArrayList<>();

        for (ExternalDocumentSaveRequest.MetadataRequest meta : metadata) {
            DocumentMetadata dm = new DocumentMetadata();
            dm.setDocumentHeader(header);
            dm.setMetaKey(meta.getKey());
            dm.setMetaValue(meta.getValue());
            dm.setCreatedOn(new Timestamp(System.currentTimeMillis()));
            metaList.add(dm);
        }
        header.setMetadataList(metaList);
    }




    // ===============================
    // GET APPROVED
    // ===============================
    @Override
    @Transactional(readOnly = true)
    public List<DocumentViewResponse> getApprovedDocuments(
            Employee employee,
            BranchMaster branch,
            DepartmentMaster department) {

        List<DocumentHeader> headers =
                headerRepository.findByApprovalStatusAndEmployeeAndBranchMasterAndDepartmentMaster(
                        DocApprovalStatus.APPROVED,
                        employee,
                        branch,
                        department
                );

        return headers.stream()
                .map(this::mapToDocumentViewResponse)
                .toList();
    }


    // ===============================
    // GET BY ID
    // ===============================
    @Override
    @Transactional(readOnly = true)
    public DocumentViewResponse getDocumentViewById(Integer id, Employee employee) {

        DocumentHeader header = headerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found with id: " + id));

        if (!header.getBranchMaster().getId().equals(employee.getBranch().getId())
                || !header.getDepartmentMaster().getId().equals(employee.getDepartment().getId())) {

            throw new RuntimeException("Access denied for this document");
        }

        return mapToDocumentViewResponse(header);
    }


    // ===============================
    // ADVANCED SEARCH
    // ===============================
    @Transactional(readOnly = true)
    @Override
    public Page<DocumentViewResponse> searchDocuments(
            ExDocumentSearchRequest request,
            BranchMaster branch,
            DepartmentMaster department) {

        Specification<DocumentHeader> spec = filterDocuments(request, branch.getId(), department.getId());

        Sort sort = Sort.by(Sort.Direction.fromString(request.getSortDir()), request.getSortBy());
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);

        Page<DocumentHeader> headersPage = headerRepository.findAll(spec, pageable);

        List<DocumentViewResponse> responses = headersPage.stream()
                .map(this::mapToDocumentViewResponse)
                .toList();

        return new PageImpl<>(responses, pageable, headersPage.getTotalElements());
    }



    // ===============================
    // ADVANCED DOWNLOAD
    // ===============================
    @Transactional(readOnly = true)
    @Override
    public AdvancedDownloadResponse preCheckFilesByIds(
            AdvancedDownloadByIdsRequest request, Employee employee) {

        List<DocumentHeader> headers =
                headerRepository.findAllByIdAndBranchAndDepartment(
                        request.getDocumentIds(),
                        employee.getBranch(),
                        employee.getDepartment()
                );

        return calculateFileSize(headers);
    }



    @Transactional(readOnly = true)
    public AdvancedDownloadResponse preCheckFilesByDate(
            AdvancedDownloadByDateRequest request, Employee employee) {

        List<DocumentHeader> headers =
                headerRepository.findHeadersByDetailApprovedDateBetweenAndBranchAndDepartment(
                        request.getFromDate(),
                        request.getToDate(),
                        employee.getBranch(),
                        employee.getDepartment()
                );

        return calculateFileSize(headers);
    }


    @Override
    public InputStreamResource prepareDownloadByIds(AdvancedDownloadByIdsRequest request, Employee employee) throws IOException {

        List<DocumentHeader> headers = headerRepository.findAllByIdAndBranchAndDepartment(
                request.getDocumentIds(),
                employee.getBranch(),
                employee.getDepartment()
        );

        if (request.getDownloadType().isFileIncluded()) {
            boolean hasArchived = headers.stream()
                    .flatMap(h -> h.getDocumentDetails().stream())
                    .anyMatch(DocumentDetails::getLtoArchived);

            if (hasArchived) {
                return null;
            }
        }

        return buildDownload(headers, request.getDownloadType());
    }

    @Override
    public InputStreamResource prepareDownloadByDate(AdvancedDownloadByDateRequest request, Employee employee) throws IOException {

        List<DocumentHeader> headers = headerRepository.findHeadersByDetailApprovedDateBetweenAndBranchAndDepartment(
                request.getFromDate(),
                request.getToDate(),
                employee.getBranch(),
                employee.getDepartment()
        );

        if (request.getDownloadType().isFileIncluded()) {
            boolean hasArchived = headers.stream()
                    .flatMap(h -> h.getDocumentDetails().stream())
                    .anyMatch(DocumentDetails::getLtoArchived);

            if (hasArchived) {
                return null;
            }
        }

        return buildDownload(headers, request.getDownloadType());
    }

    private AdvancedDownloadResponse calculateFileSize(List<DocumentHeader> headers) {
        long totalBytes = 0;
        int fileCount = 0;

        for (DocumentHeader header : headers) {
            for (DocumentDetails d : header.getDocumentDetails()) {
                totalBytes += Long.parseLong(d.getFileSizeBytes());
                fileCount++;
            }
        }

        AdvancedDownloadResponse response = new AdvancedDownloadResponse();
        response.setTotalFiles(fileCount);
        response.setTotalFileSizeBytes(totalBytes);
        response.setTotalFileSizeHuman(readableFileSize(totalBytes));
        response.setMessage(
                "Files will be downloaded. Please confirm by setting confirmFiles=true"
        );
        return response;
    }

    @Override
    public String getDownloadFilename(DownloadType type) {
        return switch (type) {
            case DATA_ONLY -> "data.json";
            case FILES_ONLY -> "files.zip";
            case DATA_WITH_FILES -> "data_with_files.zip";
        };
    }


    private String readableFileSize(long size) {
        if (size <= 0) return "0 B";

        final String[] units = {"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));

        return new DecimalFormat("#,##0.#")
                .format(size / Math.pow(1024, digitGroups))
                + " " + units[digitGroups];
    }

    @Transactional
    public Map<String, Object> uploadFilesUnique(
            List<MultipartFile> files,
            String branch, String department,
            String category, String year,
            String version,
            String fileNo) {

        branch = sanitizeSegment(branch);
        department = sanitizeSegment(department);
        category = sanitizeSegment(category);
        year = sanitizeSegment(year);
        version = sanitizeSegment(version);

        String relativeDir = String.format("%s/%s/%s/%s/%s",
                branch, department, year, category, version);

        final long MAX_FILE_BYTES = filesizeUploads;

        List<String> activeExt = filesTypeMasterRepository.findActiveFileExtensions();
        Set<String> allowed = activeExt.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        List<Map<String, Object>> uploaded = new CopyOnWriteArrayList<>();
        List<Map<String, String>> errors = new CopyOnWriteArrayList<>();

        DocHelper docHelper = new DocHelper();
        AtomicInteger sequence = new AtomicInteger(1);

        for (MultipartFile f : files) {
            if (f == null || f.isEmpty()) continue;

            String original = Optional.ofNullable(f.getOriginalFilename()).orElse("unknown");

            try {
                if (f.getSize() > MAX_FILE_BYTES) {
                    errors.add(Map.of("file", original, "error", "File too large"));
                    continue;
                }

                String ext = extOf(original).toLowerCase();
                if (!allowed.contains(ext)) {
                    errors.add(Map.of("file", original, "error", "Unsupported file type"));
                    continue;
                }

                String finalName = buildStructuredFileName(
                        fileNo, category, year, version,
                        sequence.getAndIncrement(), ext
                );

                Path targetDir = Paths.get(basePath, relativeDir);
                Files.createDirectories(targetDir);

                Path tempFile = Files.createTempFile("upload-", "." + ext);
                Files.copy(f.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);

                int pageCount = docHelper.getPageCount(tempFile, ext);

                Path encryptedFile = targetDir.resolve(finalName);
                try (InputStream in = Files.newInputStream(tempFile);
                     OutputStream out = Files.newOutputStream(
                             encryptedFile,
                             StandardOpenOption.CREATE,
                             StandardOpenOption.TRUNCATE_EXISTING)) {

                    fileEncryptionUtil.encrypt(in, out);
                }

                Files.deleteIfExists(tempFile);

                long size = Files.size(encryptedFile);

                Map<String, Object> meta = new LinkedHashMap<>();
                meta.put("originalName", original);
                meta.put("storedName", finalName);
                meta.put("path", relativeDir + "/" + finalName);
                meta.put("fileSizeBytes", size);
                meta.put("fileSizeHuman", docHelper.humanReadableSize(size));
                meta.put("fileType", ext);
                meta.put("pageCount", pageCount);

                uploaded.add(meta);

            } catch (Exception ex) {
                errors.add(Map.of("file", original, "error", ex.getMessage()));
            }
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("uploadedFiles", uploaded);
        resp.put("errors", errors);
        return resp;
    }


    private static String sanitizeSegment(String s) {
        if (s == null) return "NA";
        String t = s.trim().replace(" ", "_").replaceAll("[^a-zA-Z0-9._-]", "_");
        return t.isBlank() ? "NA" : t;
    }

    private static String extOf(String filename) {
        int i = filename.lastIndexOf('.');
        return (i >= 0) ? filename.substring(i) : "";
    }

    private String buildStructuredFileName(
            String fileNo,
            String category,
            String year,
            String version,
            int sequence,
            String ext) {

        String prefix = (fileNo != null && fileNo.length() >= 3)
                ? fileNo.substring(0, 3).toUpperCase()
                : "AGT";

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmssSSS")
                .format(new Date());

        return String.format("%s_%s_%s_%s_%s_%d.%s",
                prefix,
                sanitizeSegment(category),
                year,
                version,
                timestamp,
                sequence,
                ext);
    }


    private DocumentViewResponse mapToDocumentViewResponse(DocumentHeader header) {

        DocumentViewResponse response = new DocumentViewResponse();

        // ───── Header Info ─────
        response.setDocumentId(header.getId());
        response.setTitle(header.getTitle());
        response.setFileNo(header.getFileNo());
        response.setSubject(header.getSubject());
        response.setCategoryName(header.getCategoryMaster().getName());
        response.setDepartmentName(header.getDepartmentMaster().getName());
        response.setBranchName(header.getBranchMaster().getName());
        response.setEmployeeName(header.getEmployee().getName());

        // ───── Audit Info ─────
        response.setCreatedOn(header.getCreatedOn());
        response.setCreatedBy(header.getCreatedBy());
        response.setUpdatedOn(header.getUpdatedOn());
        response.setUpdatedBy(header.getUpdatedBy());

        // ───── Files ─────
        response.setFiles(
                header.getDocumentDetails().stream().map(file -> {
                    DocumentViewResponse.FileInfo fi =
                            new DocumentViewResponse.FileInfo();

                    fi.setFileId(file.getId());
                    fi.setFileName(file.getDocName());
                    fi.setVersion(file.getVersion());
                    fi.setYear(file.getYearMaster().getName());
                    fi.setMimeType(file.getMimeType());
                    fi.setFileType(file.getFileType());
                    fi.setPageCounts(file.getPageCounts());
                    fi.setStatus(file.getStatus());
                    fi.setFileSizeHuman(file.getFileSizeHuman());
                    fi.setApprovedDate(file.getApprovedOn());
                    return fi;
                }).toList()
        );

        // ───── Metadata ─────
        response.setMetadata(
                header.getMetadataList().stream().map(meta -> {
                    DocumentViewResponse.MetadataInfo mi =
                            new DocumentViewResponse.MetadataInfo();

                    mi.setKey(meta.getMetaKey());
                    mi.setValue(meta.getMetaValue());
                    mi.setCreatedOn(meta.getCreatedOn());

                    return mi;
                }).toList()
        );

        return response;
    }

    private InputStreamResource buildDownload(
            List<DocumentHeader> headers,
            DownloadType downloadType) throws IOException {

        List<DocumentViewResponse> viewResponses =
                headers.stream()
                        .map(this::mapToDocumentViewResponse)
                        .toList();

        // ================= DATA ONLY =================
        if (downloadType == DownloadType.DATA_ONLY) {
            File json = File.createTempFile("data_", ".json");
            json.deleteOnExit();
            objectMapper.writeValue(json, viewResponses);
            return new InputStreamResource(new FileInputStream(json));
        }

        File zip = File.createTempFile("download_", ".zip");
        zip.deleteOnExit();

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip))) {

            // ---------- JSON inside ZIP ----------
            if (downloadType == DownloadType.DATA_WITH_FILES) {
                zos.putNextEntry(new ZipEntry("data.json"));

                // ❗ DO NOT let Jackson close ZipOutputStream
                JsonGenerator generator =
                        objectMapper.getFactory().createGenerator(zos);
                generator.writeObject(viewResponses);
                generator.flush();   // IMPORTANT
                // DO NOT close generator

                zos.closeEntry();
            }

            // ---------- Decrypted files ----------
            for (DocumentHeader h : headers) {
                for (DocumentDetails d : h.getDocumentDetails()) {

                    File encryptedFile = new File(basePath, d.getPath());

                    if (!encryptedFile.exists()) {
                        log.warn("Missing file: {}", encryptedFile.getAbsolutePath());
                        continue;
                    }

                    String zipPath = h.getId() + "/" + encryptedFile.getName();
                    zos.putNextEntry(new ZipEntry(zipPath));

                    InputStream fis = null;
                    CipherInputStream cis = null;

                    try {
                        fis = new FileInputStream(encryptedFile);
                        cis = fileEncryptionUtil.decrypt(fis);

                        cis.transferTo(zos);

                    } catch (Exception e) {
                        throw new IllegalStateException(
                                "Failed to decrypt file: " + encryptedFile.getName(), e
                        );
                    } finally {
                        // ❗ DO NOT close cis (it would close zos)
                        if (fis != null) {
                            fis.close();
                        }
                    }

                    zos.closeEntry();
                }
            }
        }

        return new InputStreamResource(new FileInputStream(zip));
    }


    // search helper
    public static Specification<DocumentHeader> filterDocuments(
            ExDocumentSearchRequest request, Integer branchId, Integer departmentId) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Mandatory filters
            predicates.add(cb.equal(root.get("branchMaster").get("id"), branchId));
            predicates.add(cb.equal(root.get("departmentMaster").get("id"), departmentId));
            predicates.add(cb.equal(root.get("approvalStatus"), DocApprovalStatus.APPROVED));

            // Optional filters
            if (request.getFileNo() != null) {
                predicates.add(cb.like(cb.lower(root.get("fileNo")), "%" + request.getFileNo().toLowerCase() + "%"));
            }
            if (request.getTitle() != null) {
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + request.getTitle().toLowerCase() + "%"));
            }
            if (request.getSubject() != null) {
                predicates.add(cb.like(cb.lower(root.get("subject")), "%" + request.getSubject().toLowerCase() + "%"));
            }
            if (request.getCategory() != null) {
                Join<DocumentHeader, CategoryMaster> categoryJoin = root.join("categoryMaster", JoinType.LEFT);
                predicates.add(cb.like(cb.lower(categoryJoin.get("name")), "%" + request.getCategory().toLowerCase() + "%"));
            }
            if (request.getYear() != null) {
                Join<DocumentHeader, DocumentDetails> detailsJoin = root.join("documentDetails", JoinType.LEFT);
                Join<DocumentDetails, YearMaster> yearJoin = detailsJoin.join("yearMaster", JoinType.LEFT);
                predicates.add(cb.like(cb.lower(yearJoin.get("name")), "%" + request.getYear().toLowerCase() + "%"));
            }

            // Metadata filters
            if (request.getMetadata() != null && !request.getMetadata().isEmpty()) {
                Join<DocumentHeader, DocumentMetadata> metadataJoin = root.join("metadataList", JoinType.LEFT);
                List<Predicate> metadataPredicates = new ArrayList<>();
                for (ExDocumentSearchRequest.MetadataFilter filter : request.getMetadata()) {
                    Predicate key = cb.equal(cb.lower(metadataJoin.get("metaKey")), filter.getKey().toLowerCase());
                    Predicate value = cb.like(cb.lower(metadataJoin.get("metaValue")), "%" + filter.getValue().toLowerCase() + "%");
                    metadataPredicates.add(cb.and(key, value));
                }
                predicates.add(cb.or(metadataPredicates.toArray(new Predicate[0])));
            }

            query.distinct(true);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }


    // restore

    @Override
    @Transactional
    public void restoreDocuments(
            ExternalRestoreRequest request,
            Employee employee
    ) {

        List<DocumentDetails> archivedDetails;

        if (request.getRestoreType() == ExternalRestoreRequest.RestoreType.BY_IDS) {
            archivedDetails =
                    getArchivedDetailsByHeaderIds(
                            request.getHeaderIds(),
                            employee
                    );
        } else {
            archivedDetails =
                    getArchivedDetailsByDateRange(
                            request.getFromDate(),
                            request.getToDate(),
                            employee
                    );
        }

        if (archivedDetails.isEmpty()) {
            throw new RuntimeException("No archived documents found to restore");
        }

        List<Integer> documentDetailIds =
                archivedDetails.stream()
                        .map(DocumentDetails::getId)
                        .distinct()
                        .toList();

        p5RestoreApiService.restoreByDocumentDetailsIds(
                documentDetailIds,
                basePath
        );
    }


    private List<DocumentDetails> getArchivedDetailsByHeaderIds(
            List<Integer> headerIds,
            Employee employee
    ) {

        List<DocumentHeader> headers =
                headerRepository.findAllByIdAndBranchAndDepartment(
                        headerIds,
                        employee.getBranch(),
                        employee.getDepartment()
                );

        return headers.stream()
                .flatMap(h -> h.getDocumentDetails().stream())
                .filter(DocumentDetails::getLtoArchived)
                .toList();
    }


    private List<DocumentDetails> getArchivedDetailsByDateRange(
            LocalDate fromDate,
            LocalDate toDate,
            Employee employee
    ) {

        List<DocumentHeader> headers =
                headerRepository.findHeadersByDetailApprovedDateBetweenAndBranchAndDepartment(
                        fromDate,
                        toDate,
                        employee.getBranch(),
                        employee.getDepartment()
                );

        return headers.stream()
                .flatMap(h -> h.getDocumentDetails().stream())
                .filter(DocumentDetails::getLtoArchived)
                .toList();
    }


}
