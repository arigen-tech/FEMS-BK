package com.dmsBackend.service.Impl;

import com.dmsBackend.entity.*;
import com.dmsBackend.exception.ResourceNotFoundException;
import com.dmsBackend.repository.*;
import com.dmsBackend.response.*;
import com.dmsBackend.service.*;
import com.dmsBackend.utils.AuditLogUtil;
import com.dmsBackend.utils.CurrentUser;
import com.dmsBackend.utils.DocHelper;
import com.dmsBackend.utils.ResponseUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@Transactional
public class DocumentHeaderServiceImpl implements DocumentHeaderService {

    private final DocumentHeaderRepository documentHeaderRepository;
    private final CategoryMasterRepository categoryMasterRepository;
    @Autowired
    private  CaseTypeMasterRepository caseTypeMasterRepository;
    @Autowired
    private  CrimeTypeMasterRepository crimeTypeMasterRepository;
    @Autowired
    private  StateMasterRepository stateMasterRepository;
    @Autowired
    private  DistrictMasterRepository districtMasterRepository;
    @Autowired
    private  CityMasterRepository cityMasterRepository;
    @Autowired
    private PriorityMasterRepository priorityMasterRepository;


    @Autowired
    private PDFGenerator pdfGenerator;
    private final String basePath = "C:/DMSData/";

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ExcelsGenerator excelGenerator;

    @Autowired
    private DocHelper docHelper; // Use the Spring-managed bean

    private final YearMasterRepository yearMasterRepository;
    private final EmployeeRepository employeeRepository;

    @Autowired
    private BranchMasterService branchMasterService;

    @Autowired
    private CurrentUser currentUser;

    @Autowired
    private AuditLogUtil auditLogUtil;

    @Autowired
    WaitingRoomScheduler waitingRoomScheduler;

    @Autowired
    WaitingRoomRepository waitingRoomRepository;

    @Autowired
    DocumentMetadataRepository documentMetadataRepository;

    @Autowired
    private DocumentForwardingAuthorityRepository forwardingAuthorityRepository;

    @Autowired
    private ForwardingAuthorityTypeMasterRepository forwardingAuthorityTypeRepository;



    @Autowired
    private ModeOfSubmissionMasterRepository modeOfSubmissionRepository;

    @Autowired
    private PackageTypeMasterRepository packageTypeRepository;

    @Value("${document.storage.path}")
    private String documentStoragePath;

    @Value("${waitingroom.storage.path}")
    private String waitingRoomStoragePath;

    private final DocumentDetailsService documentDetailsService;
    private final DocumentDetailsRepository documentDetailsRepository;
    private final DepartmentMasterRepository departmentMasterRepository;
    private final BranchMasterRepository branchMasterRepository;

    private final DocumentsAuditLogService documentsAuditLogService;
    private final DocHeaderStatusService docHeaderStatusService;
    @Value("${forwarding.letter.storage.path:${document.storage.path}/forwarding-letters}")
    private String forwardingLetterStoragePath;

    @Autowired
    private QRCodeGenerator qrCodeGenerator;

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Autowired
    public DocumentHeaderServiceImpl(DocumentHeaderRepository documentHeaderRepository,
                                     DepartmentMasterRepository departmentMasterRepository,
                                     BranchMasterRepository branchMasterRepository,
                                     DocumentDetailsService documentDetailsService,
                                     CategoryMasterRepository categoryMasterRepository,
                                     YearMasterRepository yearMasterRepository,
                                     EmployeeRepository employeeRepository,
                                     DocumentDetailsRepository documentDetailsRepository,
                                     DocumentsAuditLogService documentsAuditLogService,
                                     DocHeaderStatusService docHeaderStatusService,
                                     DocumentMetadataRepository documentMetadataRepository) {

        this.documentHeaderRepository = documentHeaderRepository;
        this.departmentMasterRepository = departmentMasterRepository;
        this.branchMasterRepository = branchMasterRepository;
        this.categoryMasterRepository = categoryMasterRepository;
        this.yearMasterRepository = yearMasterRepository;
        this.employeeRepository = employeeRepository;
        this.documentDetailsService = documentDetailsService;
        this.documentDetailsRepository = documentDetailsRepository;
        this.docHeaderStatusService = docHeaderStatusService;
        this.documentsAuditLogService = documentsAuditLogService;
        this.documentMetadataRepository = documentMetadataRepository;
    }

    private static final Logger logger = LoggerFactory.getLogger(DocumentHeaderServiceImpl.class);

    // =============================================== Document Post, Put & Delete Operations =================================================== //

    //Save Document
//    @Override
//    public DocumentHeader saveDocumentHeader(DocumentHeader documentHeader) {
//        // Check for existing document
//        Optional<DocumentHeader> existingDocument = documentHeaderRepository.findByFileNo(documentHeader.getFileNo());
//        if (existingDocument.isPresent()) {
//            throw new ResourceConflictException("Document with fileNo " + documentHeader.getFileNo() + " already exists");
//        }
//
//        // Validate and set relationships
//        if (documentHeader.getCategoryMaster() != null && documentHeader.getCategoryMaster().getId() != null) {
//            CategoryMaster categoryMaster = categoryMasterRepository.findById(documentHeader.getCategoryMaster().getId())
//                    .orElseThrow(() -> new ResourceNotFoundException("CategoryMaster not found with id " + documentHeader.getCategoryMaster().getId()));
//            documentHeader.setCategoryMaster(categoryMaster);
//        } else {
//            throw new IllegalArgumentException("CategoryMaster ID must not be null");
//        }
//
//        if (documentHeader.getYearMaster() != null && documentHeader.getYearMaster().getId() != null) {
//            YearMaster yearMaster = yearMasterRepository.findById(documentHeader.getYearMaster().getId())
//                    .orElseThrow(() -> new ResourceNotFoundException("YearMaster not found with id " + documentHeader.getYearMaster().getId()));
//            documentHeader.setYearMaster(yearMaster);
//        } else {
//            throw new IllegalArgumentException("YearMaster ID must not be null");
//        }
//
//        // Set default values
//        Timestamp now = new Timestamp(System.currentTimeMillis());
//        documentHeader.setCreatedOn(now);
//        documentHeader.setUpdatedOn(now);
//        documentHeader.setActive(true);
//        documentHeader.setApprovalStatus(DocApprovalStatus.PENDING);
//
//        // Save temporarily to generate ID
//        DocumentHeader savedDocumentHeader = documentHeaderRepository.save(documentHeader);
//
//        // Generate QR code path
//        try {
//            String qrCodePath = qrCodeGenerator.generateQRCodeForDocument(savedDocumentHeader);
//            savedDocumentHeader.setQrPath(qrCodePath);
//        } catch (RuntimeException e) {
//            savedDocumentHeader.setQrPath(null);
//        }
//
//        return documentHeaderRepository.save(savedDocumentHeader);
//    }

    @Transactional
    @Override
    public ApiResponse<Map<String, Object>> saveDocumentWithFiles(DocumentSaveRequest req,
                                                                  HttpServletRequest request) {
        log.info("API CALL → Save Document With Files | employeeId={}",
                req.getDocumentHeader().getEmployee() != null ? req.getDocumentHeader().getEmployee().getId() : "null");

        ApiResponse<Map<String, Object>> api = new ApiResponse<>();
        List<Integer> waitingRoomIdsToRollback = new ArrayList<>();

        try {
            // 1️⃣ Resolve full employee
            DocumentHeader header = req.getDocumentHeader();
            Employee emp = employeeRepository.findById(header.getEmployee().getId())
                    .orElseThrow(() -> {
                        log.error("FAILED → Save Document | reason=Employee Not Found | employeeId={}", header.getEmployee().getId());
                        return new ResourceNotFoundException("Employee not found");
                    });
            header.setEmployee(emp);
            header.setBranchMaster(emp.getBranch());
            header.setDepartmentMaster(emp.getDepartment());

            // 1️⃣.1️⃣ Resolve full category
            if (header.getCategoryMaster() != null && header.getCategoryMaster().getId() != null) {
                CategoryMaster category = categoryMasterRepository.findById(header.getCategoryMaster().getId())
                        .orElseThrow(() -> {
                            log.error("FAILED → Save Document | reason=Category Not Found | categoryId={}", header.getCategoryMaster().getId());
                            return new ResourceNotFoundException("Category not found");
                        });
                header.setCategoryMaster(category);
            }

            // 1️⃣.2️⃣ Resolve full Case Type
            if (header.getCaseType() != null && header.getCaseType().getId() != null) {
                CaseTypeMaster caseType = caseTypeMasterRepository.findById(header.getCaseType().getId())
                        .orElseThrow(() -> {
                            log.error("FAILED → Save Document | reason=CaseType Not Found | caseTypeId={}", header.getCaseType().getId());
                            return new ResourceNotFoundException("CaseTypeMaster not found with id " + header.getCaseType().getId());
                        });
                header.setCaseType(caseType);
            }

            // 1️⃣.3️⃣ Resolve full Crime Type
            if (header.getCrimeType() != null && header.getCrimeType().getId() != null) {
                CrimeTypeMaster crimeType = crimeTypeMasterRepository.findById(header.getCrimeType().getId())
                        .orElseThrow(() -> {
                            log.error("FAILED → Save Document | reason=CrimeType Not Found | crimeTypeId={}", header.getCrimeType().getId());
                            return new ResourceNotFoundException("CrimeTypeMaster not found with id " + header.getCrimeType().getId());
                        });
                header.setCrimeType(crimeType);
            }

            // 1️⃣.4️⃣ Resolve full State
            if (header.getState() != null && header.getState().getId() != null) {
                StateMaster state = stateMasterRepository.findById(header.getState().getId())
                        .orElseThrow(() -> new ResourceNotFoundException("StateMaster not found with id " + header.getState().getId()));
                header.setState(state);
            }

            // 1️⃣.5️⃣ Resolve full District
            if (header.getDistrict() != null && header.getDistrict().getId() != null) {
                DistrictMaster district = districtMasterRepository.findById(header.getDistrict().getId())
                        .orElseThrow(() -> new ResourceNotFoundException("DistrictMaster not found with id " + header.getDistrict().getId()));
                header.setDistrict(district);
            }

            // 1️⃣.6️⃣ Resolve full City
            if (header.getCity() != null && header.getCity().getId() != null) {
                CityMaster city = cityMasterRepository.findById(header.getCity().getId())
                        .orElseThrow(() -> new ResourceNotFoundException("CityMaster not found with id " + header.getCity().getId()));
                header.setCity(city);
            }

            // 1️⃣.7️⃣ Resolve full Priority
            if (header.getPriority() != null && header.getPriority().getId() != null) {
                PriorityMaster priority = priorityMasterRepository.findById(header.getPriority().getId())
                        .orElseThrow(() -> new ResourceNotFoundException("PriorityMaster not found with id " + header.getPriority().getId()));
                header.setPriority(priority);
            }

            // 2️⃣ Extract waiting room IDs from file paths
            List<Integer> waitingRoomIdsInRequest = req.getFilePaths().stream()
                    .filter(fp -> fp.getWaitingRoomId() != null)
                    .map(fp -> fp.getWaitingRoomId())
                    .collect(Collectors.toList());

            waitingRoomIdsToRollback.addAll(waitingRoomIdsInRequest);

            log.debug("Found {} waiting room files to process", waitingRoomIdsInRequest.size());

            // 3️⃣ MOVE WAITING ROOM FILES TO DOCUMENT STORAGE
            if (!waitingRoomIdsInRequest.isEmpty()) {
                log.info("Moving {} waiting room files to document storage", waitingRoomIdsInRequest.size());
                moveWaitingRoomFilesToDocumentStorage(req.getFilePaths(), emp, header.getCategoryMaster());
            }

            // 4️⃣ Init audit fields
            Timestamp now = new Timestamp(System.currentTimeMillis());
            header.setCreatedOn(now);
            header.setUpdatedOn(now);
            header.setApprovalStatus(DocApprovalStatus.PENDING);
            header.setCreatedBy(emp.getEmail());
            header.setUpdatedBy(emp.getEmail());

            header.setCaseId(null);

            log.info("Saving document header");
            DocumentHeader savedHeader = documentHeaderRepository.save(header);

            // 5️⃣ Generate QR code AND auto-generate Case ID
            try {
                String qrCodePath = qrCodeGenerator.generateQRCodeForDocument(savedHeader);
                savedHeader.setQrPath(qrCodePath);
                log.debug("QR code generated for document {}", savedHeader.getId());
            } catch (Exception e) {
                log.error("QR code generation failed for header {}", savedHeader.getId(), e);
                savedHeader.setQrPath(null);
            }

            String generatedCaseId = "CASE-" + java.time.Year.now().getValue()
                    + "-" + String.format("%06d", savedHeader.getId());
            savedHeader.setCaseId(generatedCaseId);
            log.info("Auto-generated Case ID | documentId={} caseId={}", savedHeader.getId(), generatedCaseId);

            savedHeader = documentHeaderRepository.save(savedHeader);

            // 6️⃣ Save file details
            log.info("Saving file details for document {}", savedHeader.getId());
            documentDetailsService.saveFileDetailsWithWaitingRoom(
                    savedHeader, req.getFilePaths(), emp.getEmail());

            // 6️⃣.1️⃣ Save Forwarding Authority Details (one row per document)
            if (req.getForwardingAuthority() != null) {
                DocumentSaveRequest.ForwardingAuthorityRequest fa = req.getForwardingAuthority();

                DocumentForwardingAuthority forwardingAuthority = new DocumentForwardingAuthority();
                forwardingAuthority.setDocumentHeader(savedHeader);

                if (fa.getForwardingAuthorityTypeId() != null) {
                    ForwardingAuthorityTypeMaster authorityType = forwardingAuthorityTypeRepository
                            .findById(fa.getForwardingAuthorityTypeId())
                            .orElse(null);
                    forwardingAuthority.setForwardingAuthorityType(authorityType);
                }

                forwardingAuthority.setAuthorityName(fa.getAuthorityName());
                forwardingAuthority.setDesignation(fa.getDesignation());
                forwardingAuthority.setOrganisation(fa.getOrganisation());

                if (fa.getDistrictId() != null) {
                    DistrictMaster district = districtMasterRepository
                            .findById(fa.getDistrictId())
                            .orElse(null);
                    forwardingAuthority.setDistrict(district);
                }

                if (fa.getCityId() != null) {
                    CityMaster city = cityMasterRepository
                            .findById(fa.getCityId())
                            .orElse(null);
                    forwardingAuthority.setCity(city);
                }

                forwardingAuthority.setAddress(fa.getAddress());
                forwardingAuthority.setContactNumber(fa.getContactNumber());
                forwardingAuthority.setEmail(fa.getEmail());
                forwardingAuthority.setForwardingLetterNumber(fa.getForwardingLetterNumber());
                forwardingAuthority.setForwardingDate(fa.getForwardingDate());
                forwardingAuthority.setForwardingLetterPath(fa.getForwardingLetterPath());

                if (fa.getModeOfSubmissionId() != null) {
                    ModeOfSubmissionMaster modeOfSubmission = modeOfSubmissionRepository
                            .findById(fa.getModeOfSubmissionId())
                            .orElse(null);
                    forwardingAuthority.setModeOfSubmission(modeOfSubmission);
                }

                forwardingAuthority.setCourierAgency(fa.getCourierAgency());
                forwardingAuthority.setAwbConsignmentNumber(fa.getAwbConsignmentNumber());
                forwardingAuthority.setBookingDate(fa.getBookingDate());
                forwardingAuthority.setDispatchDate(fa.getDispatchDate());
                forwardingAuthority.setExpectedDeliveryDate(fa.getExpectedDeliveryDate());
                forwardingAuthority.setActualDeliveryDate(fa.getActualDeliveryDate());
                forwardingAuthority.setParcelId(fa.getParcelId());
                forwardingAuthority.setParcelNumber(fa.getParcelNumber());
                forwardingAuthority.setNumberOfExhibits(fa.getNumberOfExhibits());

                if (fa.getPackageTypeId() != null) {
                    PackageTypeMaster packageType = packageTypeRepository
                            .findById(fa.getPackageTypeId())
                            .orElse(null);
                    forwardingAuthority.setPackageType(packageType);
                }

                forwardingAuthority.setSealNumber(fa.getSealNumber());
                forwardingAuthority.setSealDescription(fa.getSealDescription());
                forwardingAuthority.setSealCondition(fa.getSealCondition());
                forwardingAuthority.setPackageCondition(fa.getPackageCondition());
                forwardingAuthority.setReceivedDate(fa.getReceivedDate());
                forwardingAuthority.setReceivedTime(fa.getReceivedTime());
                forwardingAuthority.setReceivedBy(fa.getReceivedBy());
                forwardingAuthority.setRemarks(fa.getRemarks());

                // NEW — messenger/handover fields
                forwardingAuthority.setMessengerName(fa.getMessengerName());
                forwardingAuthority.setMessengerDesignation(fa.getMessengerDesignation());
                forwardingAuthority.setMessengerOrganization(fa.getMessengerOrganization());
                forwardingAuthority.setMessengerIdRef(fa.getMessengerIdRef());
                forwardingAuthority.setHandoverDateTime(fa.getHandoverDateTime());

                forwardingAuthority.setCreatedBy(emp.getEmail());
                forwardingAuthority.setCreatedOn(now);

                forwardingAuthorityRepository.save(forwardingAuthority);
                log.info("Saved Forwarding Authority Details for document {}", savedHeader.getId());
            }

            // 7️⃣ Recalculate header status
            docHeaderStatusService.recalcAndUpdateHeaderStatus(savedHeader, emp.getEmail());
            documentHeaderRepository.save(savedHeader);

            // 5️⃣.1️⃣ Save metadata (SEARCHABLE)
            if (req.getMetadata() != null && !req.getMetadata().isEmpty()) {
                List<DocumentMetadata> metaList = new ArrayList<>();

                for (DocumentSaveRequest.MetadataRequest m : req.getMetadata()) {
                    if (m.getKey() == null || m.getKey().isBlank()) continue;

                    DocumentMetadata meta = new DocumentMetadata();
                    meta.setDocumentHeader(savedHeader);
                    meta.setMetaKey(m.getKey().trim());
                    meta.setMetaValue(m.getValue());
                    meta.setCreatedOn(now);

                    metaList.add(meta);
                }

                savedHeader.getMetadataList().addAll(metaList);
                log.debug("Saved {} metadata entries for document {}", metaList.size(), savedHeader.getId());
            }

            // 8️⃣ Update waiting room status to MOVED
            if (!waitingRoomIdsInRequest.isEmpty()) {
                waitingRoomScheduler.updateStatusToMoved(waitingRoomIdsInRequest);
                log.info("Updated {} waiting room files to MOVED status", waitingRoomIdsInRequest.size());
            }

            // 9️⃣ Clear rollback list since save was successful
            waitingRoomIdsToRollback.clear();

            // 🔟 Fetch all saved details
            List<DocumentDetailsResponse> details =
                    documentDetailsService.findDocumentsByHeaderId(savedHeader.getId());

            log.debug("Fetched {} document details for header {}", details.size(), savedHeader.getId());

            // 1️⃣1️⃣ Audit log
            for (DocumentDetailsResponse fileDetail : details) {
                Map<String, Object> detailsJson = new HashMap<>();
                detailsJson.put("title", savedHeader.getTitle());
                detailsJson.put("subject", savedHeader.getSubject());
                detailsJson.put("caseId", savedHeader.getCaseId());

                if (savedHeader.getCategoryMaster() != null) {
                    detailsJson.put("category", savedHeader.getCategoryMaster().getName());
                }
                if (fileDetail.getYear() != null) {
                    detailsJson.put("year", fileDetail.getYear());
                }
                if (fileDetail.getVersion() != null) {
                    detailsJson.put("version", fileDetail.getVersion());
                }
                if (fileDetail.getDocName() != null) {
                    detailsJson.put("fileName", fileDetail.getDocName());
                }

                auditLogUtil.logDocumentAction(
                        emp,
                        "UploadDocument",
                        "Create",
                        "Success",
                        savedHeader.getId(),
                        List.of(fileDetail),
                        detailsJson,
                        request
                );
            }

            // 1️⃣2️⃣ Notifications
            try {
                notificationService.createNewDocumentSavedNotification(savedHeader);
                log.debug("Notification created for document {}", savedHeader.getId());
            } catch (Exception ex) {
                log.warn("Notification failed for header {}", savedHeader.getId(), ex);
            }

            // 1️⃣3️⃣ Prepare response
            Map<String, Object> data = new HashMap<>();
            data.put("documentHeader", savedHeader);
            data.put("documentDetails", details);

            api.setStatus(HttpStatus.OK.value());
            api.setMessage("Document and files saved successfully");
            api.setResponse(data);

            log.info("SUCCESS → Document Saved | id={} caseId={} title={} fileCount={}",
                    savedHeader.getId(), savedHeader.getCaseId(), savedHeader.getTitle(), details.size());

        } catch (Exception e) {
            log.error("FAILED → Save Document With Files | reason={}", e.getMessage(), e);

            if (!waitingRoomIdsToRollback.isEmpty()) {
                log.info("Rolling back {} waiting room files due to save failure", waitingRoomIdsToRollback.size());
                waitingRoomScheduler.updateStatusToFailed(waitingRoomIdsToRollback);
            }

            api.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            api.setMessage("Error saving document and files: " + e.getMessage());
            auditLogUtil.logDocumentAction(null, "UploadDocument", "Create", "Failure",
                    null, null, Map.of("error", e.getMessage()), request);
        }

        return api;
    }



    @Transactional
    @Override
    public ApiResponse<MessageResponse> updateDocumentWithFiles(
            DocumentHeader documentHeader,
            List<DocumentSaveRequest.MetadataRequest> metadata,
            List<Long> deletedMetaDataIds,
            List<DocumentSaveRequest.FilePathVersion> filePaths,
            DocumentSaveRequest.ForwardingAuthorityRequest forwardingAuthority,
            String version,
            HttpServletRequest request) {

        log.info("API CALL → Update Document With Files | documentId={} version={}",
                documentHeader.getId(), version);

        MessageResponse msg = new MessageResponse();
        Employee empObj = currentUser.getCurrentEmployeeOrThrow();
        List<Integer> waitingRoomIdsToRollback = new ArrayList<>();

        try {

            List<Integer> waitingRoomIdsInRequest = filePaths.stream()
                    .filter(fp -> fp.getWaitingRoomId() != null)
                    .map(DocumentSaveRequest.FilePathVersion::getWaitingRoomId)
                    .collect(Collectors.toList());

            waitingRoomIdsToRollback.addAll(waitingRoomIdsInRequest);

            log.debug("Found {} waiting room files in update request",
                    waitingRoomIdsInRequest.size());

            Optional<DocumentHeader> duplicate =
                    documentHeaderRepository.findByFileNo(
                            documentHeader.getFileNo()
                    );

            if (duplicate.isPresent()
                    && !duplicate.get().getId().equals(documentHeader.getId())) {

                log.warn(
                        "Duplicate fileNo found: {} for document {}",
                        documentHeader.getFileNo(),
                        documentHeader.getId()
                );

                msg.setMsg(
                        "Document with fileNo "
                                + documentHeader.getFileNo()
                                + " already exists"
                );

                auditLogUtil.logDocumentAction(
                        empObj,
                        "UploadDocument",
                        "Update",
                        "Failure",
                        documentHeader.getId(),
                        null,
                        Map.of("reason", "Duplicate fileNo"),
                        request
                );

                return ResponseUtils.createFailureResponse(
                        msg,
                        new TypeReference<>() {},
                        "Duplicate fileNo: "
                                + documentHeader.getFileNo(),
                        HttpStatus.CONFLICT.value()
                );
            }

            DocumentHeader existingDocument =
                    documentHeaderRepository.findById(
                            documentHeader.getId()
                    ).orElseThrow(() -> {

                        log.error(
                                "Document not found for update | documentId={}",
                                documentHeader.getId()
                        );

                        return new ResourceNotFoundException(
                                "Document not found with id "
                                        + documentHeader.getId()
                        );
                    });

            if (!waitingRoomIdsInRequest.isEmpty()) {

                log.info(
                        "Moving {} waiting room files to document storage during update",
                        waitingRoomIdsInRequest.size()
                );

                moveWaitingRoomFilesToDocumentStorage(
                        filePaths,
                        empObj,
                        existingDocument.getCategoryMaster()
                );
            }

            Map<String, Object> previousDocData = Map.of(
                    "title",
                    existingDocument.getTitle(),

                    "subject",
                    existingDocument.getSubject(),

                    "category",
                    existingDocument.getCategoryMaster() != null
                            ? existingDocument.getCategoryMaster().getName()
                            : null,

                    "year",
                    (
                            existingDocument.getDocumentDetails() != null
                                    && !existingDocument.getDocumentDetails().isEmpty()
                                    && existingDocument.getDocumentDetails()
                                    .get(0)
                                    .getYearMaster() != null
                    )
                            ? existingDocument.getDocumentDetails()
                            .get(0)
                            .getYearMaster()
                            .getName()
                            : null
            );

            List<DocumentDetailsResponse> previousFileDetails =
                    existingDocument.getDocumentDetails()
                            .stream()
                            .map(file -> {

                                DocumentDetailsResponse resp =
                                        new DocumentDetailsResponse();

                                resp.setId(file.getId());
                                resp.setDocName(file.getDocName());
                                resp.setVersion(file.getVersion());

                                return resp;
                            })
                            .toList();

            Long existingCategoryId =
                    Long.valueOf(existingDocument.getCategoryMaster() != null
                            ? existingDocument.getCategoryMaster().getId()
                            : null);

            Long incomingCategoryId =
                    Long.valueOf(documentHeader.getCategoryMaster() != null
                            ? documentHeader.getCategoryMaster().getId()
                            : null);

            boolean categoryChanged =
                    !Objects.equals(
                            existingCategoryId,
                            incomingCategoryId
                    );

            Long incomingYearId =
                    (filePaths != null && !filePaths.isEmpty())
                            ? filePaths.get(0).getYearId()
                            : null;

            boolean yearChanged =
                    incomingYearId != null
                            && existingDocument.getDocumentDetails() != null
                            && !existingDocument.getDocumentDetails().isEmpty()
                            && !existingDocument.getDocumentDetails()
                            .stream()
                            .allMatch(d ->
                                    d.getYearMaster() != null
                                            && d.getYearMaster()
                                            .getId()
                                            .equals(incomingYearId)
                            );

            log.debug(
                    "Update changes - categoryChanged: {}, yearChanged: {}",
                    categoryChanged,
                    yearChanged
            );

            if (categoryChanged
                    && documentHeader.getCategoryMaster() != null
                    && documentHeader.getCategoryMaster().getId() != null) {

                CategoryMaster categoryMaster =
                        categoryMasterRepository.findById(
                                documentHeader
                                        .getCategoryMaster()
                                        .getId()
                        ).orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "CategoryMaster not found"
                                )
                        );

                existingDocument.setCategoryMaster(
                        categoryMaster
                );
            }

            if (yearChanged && incomingYearId != null) {

                YearMaster yearMaster =
                        yearMasterRepository.findById(
                                Math.toIntExact(incomingYearId)
                        ).orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "YearMaster not found"
                                )
                        );

                for (DocumentDetails detail :
                        existingDocument.getDocumentDetails()) {

                    detail.setYearMaster(yearMaster);
                }
            }

            existingDocument.setUpdatedOn(
                    Timestamp.from(Instant.now())
            );

            existingDocument.setUpdatedBy(
                    empObj.getEmail()
            );

            existingDocument.setFileNo(
                    documentHeader.getFileNo()
            );

            existingDocument.setTitle(
                    documentHeader.getTitle()
            );

            existingDocument.setSubject(
                    documentHeader.getSubject()
            );

            existingDocument.setFirNumber(
                    documentHeader.getFirNumber()
            );

            existingDocument.setFirDate(
                    documentHeader.getFirDate()
            );

            if (documentHeader.getCaseType() != null
                    && documentHeader.getCaseType().getId() != null) {

                CaseTypeMaster caseType =
                        caseTypeMasterRepository.findById(
                                documentHeader.getCaseType().getId()
                        ).orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "CaseTypeMaster not found with id "
                                                + documentHeader
                                                .getCaseType()
                                                .getId()
                                )
                        );

                existingDocument.setCaseType(caseType);
            }

            if (documentHeader.getCrimeType() != null
                    && documentHeader.getCrimeType().getId() != null) {

                CrimeTypeMaster crimeType =
                        crimeTypeMasterRepository.findById(
                                documentHeader.getCrimeType().getId()
                        ).orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "CrimeTypeMaster not found with id "
                                                + documentHeader
                                                .getCrimeType()
                                                .getId()
                                )
                        );

                existingDocument.setCrimeType(crimeType);
            }

            if (documentHeader.getState() != null
                    && documentHeader.getState().getId() != null) {

                StateMaster state =
                        stateMasterRepository.findById(
                                documentHeader.getState().getId()
                        ).orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "StateMaster not found with id "
                                                + documentHeader
                                                .getState()
                                                .getId()
                                )
                        );

                existingDocument.setState(state);
            }

            if (documentHeader.getDistrict() != null
                    && documentHeader.getDistrict().getId() != null) {

                DistrictMaster district =
                        districtMasterRepository.findById(
                                documentHeader.getDistrict().getId()
                        ).orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "DistrictMaster not found with id "
                                                + documentHeader
                                                .getDistrict()
                                                .getId()
                                )
                        );

                existingDocument.setDistrict(district);
            }

            if (documentHeader.getCity() != null
                    && documentHeader.getCity().getId() != null) {

                CityMaster city =
                        cityMasterRepository.findById(
                                documentHeader.getCity().getId()
                        ).orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "CityMaster not found with id "
                                                + documentHeader
                                                .getCity()
                                                .getId()
                                )
                        );

                existingDocument.setCity(city);
            }

            existingDocument.setPoliceStation(
                    documentHeader.getPoliceStation()
            );

            existingDocument.setInvestigatingOfficer(
                    documentHeader.getInvestigatingOfficer()
            );

            existingDocument.setCourtReference(
                    documentHeader.getCourtReference()
            );

            if (documentHeader.getPriority() != null
                    && documentHeader.getPriority().getId() != null) {

                PriorityMaster priority =
                        priorityMasterRepository.findById(
                                documentHeader.getPriority().getId()
                        ).orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "PriorityMaster not found with id "
                                                + documentHeader
                                                .getPriority()
                                                .getId()
                                )
                        );

                existingDocument.setPriority(priority);
            }

            existingDocument.setDateOfIncident(
                    documentHeader.getDateOfIncident()
            );

            existingDocument.setIncidentLocation(
                    documentHeader.getIncidentLocation()
            );

            existingDocument.setEvidenceId(
                    documentHeader.getEvidenceId()
            );

            existingDocument.setExhibitNumber(
                    documentHeader.getExhibitNumber()
            );

            YearMaster yearMasterForFiles = null;

            if (incomingYearId != null) {

                yearMasterForFiles =
                        yearMasterRepository.findById(
                                Math.toIntExact(incomingYearId)
                        ).orElse(null);

            } else if (existingDocument.getDocumentDetails() != null
                    && !existingDocument.getDocumentDetails().isEmpty()) {

                yearMasterForFiles =
                        existingDocument
                                .getDocumentDetails()
                                .get(0)
                                .getYearMaster();
            }

            List<DocumentDetails> updatedFiles =
                    documentDetailsService.updateFileDetails(
                            existingDocument.getCategoryMaster(),
                            yearMasterForFiles,
                            existingDocument,
                            filePaths,
                            version,
                            categoryChanged || yearChanged
                    );

            existingDocument.setDocumentDetails(
                    updatedFiles
            );

            existingDocument.setApprovalStatus(
                    DocApprovalStatus.PENDING
            );

            DocumentHeader header =
                    documentHeaderRepository.save(
                            existingDocument
                    );

            msg.setMsg(
                    "Document updated successfully"
            );

            if (metadata != null) {

                log.debug(
                        "Processing {} metadata entries for update",
                        metadata.size()
                );

                Map<Long, DocumentMetadata> existingMap =
                        existingDocument
                                .getMetadataList()
                                .stream()
                                .filter(m -> m.getId() != null)
                                .collect(
                                        Collectors.toMap(
                                                DocumentMetadata::getId,
                                                m -> m
                                        )
                                );

                List<DocumentMetadata> finalList =
                        new ArrayList<>();

                for (DocumentSaveRequest.MetadataRequest m :
                        metadata) {

                    if (m.getKey() == null
                            || m.getKey().isBlank()) {

                        continue;
                    }

                    if (m.getId() > 0
                            && existingMap.containsKey(m.getId())) {

                        DocumentMetadata meta =
                                existingMap.get(m.getId());

                        meta.setMetaKey(
                                m.getKey().trim()
                        );

                        meta.setMetaValue(
                                m.getValue()
                        );

                        finalList.add(meta);

                    } else {

                        DocumentMetadata meta =
                                new DocumentMetadata();

                        meta.setDocumentHeader(
                                existingDocument
                        );

                        meta.setMetaKey(
                                m.getKey().trim()
                        );

                        meta.setMetaValue(
                                m.getValue()
                        );

                        meta.setCreatedOn(
                                new Timestamp(
                                        System.currentTimeMillis()
                                )
                        );

                        finalList.add(meta);
                    }
                }

                existingDocument
                        .getMetadataList()
                        .clear();

                existingDocument
                        .getMetadataList()
                        .addAll(finalList);
            }

            if (deletedMetaDataIds != null
                    && !deletedMetaDataIds.isEmpty()) {

                log.debug(
                        "Deleting {} metadata entries",
                        deletedMetaDataIds.size()
                );

                documentMetadataRepository.deleteAllById(
                        deletedMetaDataIds
                );
            }

            if (forwardingAuthority != null) {

                DocumentForwardingAuthority faEntity =
                        forwardingAuthorityRepository
                                .findByDocumentHeader_Id(
                                        existingDocument.getId()
                                )
                                .orElseGet(() -> {

                                    DocumentForwardingAuthority fresh =
                                            new DocumentForwardingAuthority();

                                    fresh.setDocumentHeader(
                                            existingDocument
                                    );

                                    fresh.setCreatedBy(
                                            empObj.getEmail()
                                    );

                                    fresh.setCreatedOn(
                                            Timestamp.from(
                                                    Instant.now()
                                            )
                                    );

                                    return fresh;
                                });

                if (forwardingAuthority
                        .getForwardingAuthorityTypeId() != null) {

                    ForwardingAuthorityTypeMaster authorityType =
                            forwardingAuthorityTypeRepository
                                    .findById(
                                            forwardingAuthority
                                                    .getForwardingAuthorityTypeId()
                                    )
                                    .orElse(null);

                    faEntity.setForwardingAuthorityType(
                            authorityType
                    );
                }

                faEntity.setAuthorityName(
                        forwardingAuthority.getAuthorityName()
                );

                faEntity.setDesignation(
                        forwardingAuthority.getDesignation()
                );

                faEntity.setOrganisation(
                        forwardingAuthority.getOrganisation()
                );

                if (forwardingAuthority.getDistrictId() != null) {

                    DistrictMaster district =
                            districtMasterRepository
                                    .findById(
                                            forwardingAuthority
                                                    .getDistrictId()
                                    )
                                    .orElse(null);

                    faEntity.setDistrict(district);
                }

                if (forwardingAuthority.getCityId() != null) {

                    CityMaster city =
                            cityMasterRepository
                                    .findById(
                                            forwardingAuthority
                                                    .getCityId()
                                    )
                                    .orElse(null);

                    faEntity.setCity(city);
                }

                faEntity.setAddress(
                        forwardingAuthority.getAddress()
                );

                faEntity.setContactNumber(
                        forwardingAuthority.getContactNumber()
                );

                faEntity.setEmail(
                        forwardingAuthority.getEmail()
                );

                faEntity.setForwardingLetterNumber(
                        forwardingAuthority.getForwardingLetterNumber()
                );

                faEntity.setForwardingDate(
                        forwardingAuthority.getForwardingDate()
                );

                faEntity.setForwardingLetterPath(
                        forwardingAuthority.getForwardingLetterPath()
                );

                if (forwardingAuthority.getModeOfSubmissionId() != null) {

                    ModeOfSubmissionMaster modeOfSubmission =
                            modeOfSubmissionRepository
                                    .findById(
                                            forwardingAuthority
                                                    .getModeOfSubmissionId()
                                    )
                                    .orElse(null);

                    faEntity.setModeOfSubmission(
                            modeOfSubmission
                    );
                }

                faEntity.setCourierAgency(
                        forwardingAuthority.getCourierAgency()
                );

                faEntity.setAwbConsignmentNumber(
                        forwardingAuthority.getAwbConsignmentNumber()
                );

                faEntity.setBookingDate(
                        forwardingAuthority.getBookingDate()
                );

                faEntity.setDispatchDate(
                        forwardingAuthority.getDispatchDate()
                );

                faEntity.setExpectedDeliveryDate(
                        forwardingAuthority.getExpectedDeliveryDate()
                );

                faEntity.setActualDeliveryDate(
                        forwardingAuthority.getActualDeliveryDate()
                );

                faEntity.setParcelId(
                        forwardingAuthority.getParcelId()
                );

                faEntity.setParcelNumber(
                        forwardingAuthority.getParcelNumber()
                );

                faEntity.setNumberOfExhibits(
                        forwardingAuthority.getNumberOfExhibits()
                );

                if (forwardingAuthority.getPackageTypeId() != null) {

                    PackageTypeMaster packageType =
                            packageTypeRepository
                                    .findById(
                                            forwardingAuthority
                                                    .getPackageTypeId()
                                    )
                                    .orElse(null);

                    faEntity.setPackageType(
                            packageType
                    );
                }

                faEntity.setSealNumber(
                        forwardingAuthority.getSealNumber()
                );

                faEntity.setSealDescription(
                        forwardingAuthority.getSealDescription()
                );

                faEntity.setSealCondition(
                        forwardingAuthority.getSealCondition()
                );

                faEntity.setPackageCondition(
                        forwardingAuthority.getPackageCondition()
                );

                faEntity.setReceivedDate(
                        forwardingAuthority.getReceivedDate()
                );

                faEntity.setReceivedTime(
                        forwardingAuthority.getReceivedTime()
                );

                faEntity.setReceivedBy(
                        forwardingAuthority.getReceivedBy()
                );

                faEntity.setRemarks(
                        forwardingAuthority.getRemarks()
                );

                // NEW — messenger/handover fields
                faEntity.setMessengerName(forwardingAuthority.getMessengerName());
                faEntity.setMessengerDesignation(forwardingAuthority.getMessengerDesignation());
                faEntity.setMessengerOrganization(forwardingAuthority.getMessengerOrganization());
                faEntity.setMessengerIdRef(forwardingAuthority.getMessengerIdRef());
                faEntity.setHandoverDateTime(forwardingAuthority.getHandoverDateTime());

                faEntity.setUpdatedBy(
                        empObj.getEmail()
                );

                faEntity.setUpdatedOn(
                        Timestamp.from(Instant.now())
                );

                forwardingAuthorityRepository.save(
                        faEntity
                );

                log.debug(
                        "Updated Forwarding Authority Details for document {}",
                        existingDocument.getId()
                );
            }

            docHeaderStatusService.recalcAndUpdateHeaderStatus(
                    header,
                    empObj.getEmail()
            );

            if (!waitingRoomIdsInRequest.isEmpty()) {

                waitingRoomScheduler.updateStatusToMoved(
                        waitingRoomIdsInRequest
                );

                log.info(
                        "Updated {} waiting room files to MOVED status during update",
                        waitingRoomIdsInRequest.size()
                );
            }

            waitingRoomIdsToRollback.clear();

            for (DocumentDetailsResponse prevFile :
                    previousFileDetails) {

                Map<String, Object> detailsJson =
                        Map.of(
                                "title",
                                previousDocData.get("title"),

                                "subject",
                                previousDocData.get("subject"),

                                "category",
                                previousDocData.get("category"),

                                "year",
                                previousDocData.get("year"),

                                "version",
                                prevFile.getVersion(),

                                "fileName",
                                prevFile.getDocName()
                        );

                auditLogUtil.logDocumentAction(
                        empObj,
                        "UploadDocument",
                        "Update",
                        "Success",
                        existingDocument.getId(),
                        List.of(prevFile),
                        detailsJson,
                        request
                );
            }

            log.info(
                    "SUCCESS → Document Updated | id={} caseId={} title={}",
                    existingDocument.getId(),
                    existingDocument.getCaseId(),
                    existingDocument.getTitle()
            );

            return ResponseUtils.createSuccessResponse(
                    msg,
                    new TypeReference<>() {}
            );

        } catch (Exception ex) {

            log.error(
                    "FAILED → Update Document With Files | documentId={} reason={}",
                    documentHeader.getId(),
                    ex.getMessage(),
                    ex
            );

            msg.setMsg(
                    "Document update failed"
            );

            if (!waitingRoomIdsToRollback.isEmpty()) {

                log.info(
                        "🔄 Rolling back {} waiting room files due to update failure",
                        waitingRoomIdsToRollback.size()
                );

                waitingRoomScheduler.updateStatusToFailed(
                        waitingRoomIdsToRollback
                );
            }

            auditLogUtil.logDocumentAction(
                    empObj,
                    "UploadDocument",
                    "Update",
                    "Failure",
                    documentHeader.getId(),
                    null,
                    Map.of(
                            "error",
                            ex.getMessage()
                    ),
                    request
            );

            return ResponseUtils.createFailureResponse(
                    msg,
                    new TypeReference<>() {},
                    ex.getMessage(),
                    HttpStatus.CONFLICT.value()
            );
        }
    }


    @Override
    public String saveForwardingLetterFile(MultipartFile file, Integer documentId) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        try {
            Path uploadDir;
            if (documentId != null) {
                uploadDir = Paths.get(forwardingLetterStoragePath, String.valueOf(documentId));
            } else {
                uploadDir = Paths.get(forwardingLetterStoragePath);
            }
            Files.createDirectories(uploadDir);

            String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String newFilename = "forwarding-letter-" + System.currentTimeMillis() + fileExtension;

            Path filePath = uploadDir.resolve(newFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            if (documentId != null) {
                return Paths.get(String.valueOf(documentId), newFilename).toString();
            }
            return newFilename;
        } catch (IOException e) {
            log.error("Failed to save forwarding letter file", e);
            return null;
        }
    }

    @Override
    @Transactional
    public void updateForwardingLetterPath(Integer documentId, String filePath) {
        forwardingAuthorityRepository.findByDocumentHeader_Id(documentId)
                .ifPresent(fa -> {
                    fa.setForwardingLetterPath(filePath);
                    fa.setUpdatedOn(Timestamp.from(Instant.now()));
                    forwardingAuthorityRepository.save(fa);
                });
    }

    private void moveWaitingRoomFilesToDocumentStorage(List<DocumentSaveRequest.FilePathVersion> filePaths,
                                                       Employee emp, CategoryMaster categoryMaster) {

        for (DocumentSaveRequest.FilePathVersion filePath : filePaths) {
            if (filePath.getWaitingRoomId() != null) {
                try {
                    // Get waiting room file details
                    WaitingRoom waitingRoomFile = waitingRoomRepository.findById(filePath.getWaitingRoomId())
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Waiting room file not found: " + filePath.getWaitingRoomId()));

                    // Get original file name and extension from waiting room
                    String waitingRoomFileName = Paths.get(waitingRoomFile.getFilepath()).getFileName().toString();

                    // Extract original extension properly - WITH THE DOT
                    String originalExtension = "";
                    if (waitingRoomFileName.contains(".")) {
                        originalExtension = waitingRoomFileName.substring(waitingRoomFileName.lastIndexOf("."));
                    }

                    // Build destination path structure
                    String branch = sanitizeSegment(emp.getBranch().getName());
                    String department = sanitizeSegment(emp.getDepartment().getName());
                    String category = sanitizeSegment(categoryMaster.getName());

                    // Get year
                    String year = "";
                    if (filePath.getYearId() != null) {
                        YearMaster yearMaster = yearMasterRepository.findById(filePath.getYearId().intValue())
                                .orElse(null);
                        if (yearMaster != null) {
                            year = yearMaster.getName();
                        }
                    }

                    String version = filePath.getVersion() != null ? filePath.getVersion() : "1.0";

                    // Build relative directory structure
                    String relativeDir = String.format("%s/%s/%s/%s/%s", branch, department, year, category, version);

                    // Source path in waiting room storage
                    Path sourcePath = Paths.get(waitingRoomStoragePath, waitingRoomFileName);

                    // Check if source file exists
                    if (!Files.exists(sourcePath)) {
                        throw new RuntimeException("Source file not found in waiting room: " + sourcePath);
                    }

                    // Target directory in document storage
                    Path targetDir = Paths.get(documentStoragePath, relativeDir);
                    Files.createDirectories(targetDir);

                    // Use the display name from filePath as the target file name
                    String targetFileNameWithoutExt = filePath.getPath();
                    if (targetFileNameWithoutExt.contains(".")) {
                        targetFileNameWithoutExt = targetFileNameWithoutExt.substring(0, targetFileNameWithoutExt.lastIndexOf("."));
                    }
                    String targetFileName = targetFileNameWithoutExt + originalExtension;
                    Path targetPath = targetDir.resolve(targetFileName);

                    // Move the file physically
                    Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);

                    // ✅ EXTRACT FILE METADATA USING INJECTED DOCHELPER
                    long fileSize = Files.size(targetPath);

                    // Get file extension WITH DOT for DocHelper
                    String fileExtensionWithDot = originalExtension;
                    String fileExtensionWithoutDot = getFileExtension(targetFileName); // without dot

                    String contentType = Files.probeContentType(targetPath);

                    // DEBUG: Log critical information
                    log.debug("=== WAITING ROOM FILE DEBUG ===");
                    log.debug("Source file: {}", waitingRoomFileName);
                    log.debug("Target file: {}", targetFileName);
                    log.debug("File extension with dot: {}", fileExtensionWithDot);
                    log.debug("File extension without dot: {}", fileExtensionWithoutDot);
                    log.debug("File size: {} bytes", fileSize);
                    log.debug("Content type: {}", contentType);
                    log.debug("Full target path: {}", targetPath.toAbsolutePath());

                    // Get page count using the injected DocHelper - PASS EXTENSION WITH DOT
                    int pageCount = docHelper.getPageCount(targetPath, fileExtensionWithDot);

                    log.debug("Raw page count from DocHelper: {}", pageCount);

                    // ✅ CRITICAL FIX: Handle -1 return value from DocHelper
                    if (pageCount == -1) {
                        log.warn("DocHelper returned -1 for file: {}, attempting fallback methods", targetFileName);

                        // Try without the dot in extension
                        pageCount = docHelper.getPageCount(targetPath, fileExtensionWithoutDot);
                        log.debug("Page count after fallback (without dot): {}", pageCount);

                        // If still -1, force to 1
                        if (pageCount == -1) {
                            log.warn("Fallback also failed, forcing page count to 1 for file: {}", targetFileName);
                            pageCount = 1;
                        }
                    }

                    // Final validation
                    if (pageCount <= 0) {
                        log.warn("Invalid page count {} for file {}, forcing to 1", pageCount, targetFileName);
                        pageCount = 1;
                    }

                    log.debug("Final page count: {}", pageCount);
                    log.debug("=== END DEBUG ===");

                    String humanSize = docHelper.humanReadableSize(fileSize);

                    // ✅ UPDATE FILE PATH WITH METADATA
                    filePath.setPath(relativeDir + "/" + targetFileName);
                    filePath.setFileSizeBytes(String.valueOf(fileSize));
                    filePath.setFileSizeHuman(humanSize);
                    filePath.setFileType(fileExtensionWithoutDot); // Store without dot in DB
                    filePath.setMimeType(contentType);
                    filePath.setPageCounts(pageCount);

                    log.info("Successfully moved waiting room file {} with metadata - size: {}, pages: {}",
                            filePath.getWaitingRoomId(), humanSize, pageCount);

                } catch (Exception ex) {
                    log.error("FAILED → Move Waiting Room File | waitingRoomId={} reason={}",
                            filePath.getWaitingRoomId(), ex.getMessage());
                    throw new RuntimeException("Failed to move waiting room file: " + ex.getMessage());
                }
            }
        }
    }

    // Helper method to get file extension
    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        return (lastDotIndex > 0) ? fileName.substring(lastDotIndex + 1) : "";
    }

    // Helper method to sanitize path segments
    private String sanitizeSegment(String s) {
        if (s == null) return "NA";
        String t = s.trim().replace(" ", "_").replaceAll("[^a-zA-Z0-9._-]", "_");
        return t.isBlank() ? "NA" : t;
    }

    // Add rollback method
    private void rollbackWaitingRoomFiles(List<Integer> waitingRoomIds) {
        for (Integer waitingRoomId : waitingRoomIds) {
            try {
                WaitingRoom waitingRoomFile = waitingRoomRepository.findById(waitingRoomId)
                        .orElseThrow(() -> new RuntimeException("Waiting room file not found for rollback: " + waitingRoomId));

                // The file was moved to document storage, move it back to waiting room
                String currentDocumentPath = waitingRoomFile.getFilepath(); // This should be the new path in document storage
                Path sourcePath = Paths.get(documentStoragePath, currentDocumentPath);
                Path targetPath = Paths.get(waitingRoomStoragePath, waitingRoomFile.getFilepath());

                if (Files.exists(sourcePath)) {
                    Files.createDirectories(targetPath.getParent());
                    Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    log.info("Rolled back waiting room file {} to {}", waitingRoomId, targetPath);
                }

            } catch (Exception e) {
                log.error("Failed to rollback waiting room file: " + waitingRoomId, e);
                // Don't throw exception here, continue with other files
            }
        }
    }



    //Update Document Active Status (Soft Delete)
    @Override
    public DocumentHeader updateActiveStatus(Integer id, boolean isActive) {
        log.info("API CALL → Update Document Active Status | id={} isActive={}", id, isActive);

        DocumentHeader documentHeader = findDocumentHeaderById(id);
        documentHeader.setUpdatedOn(new Timestamp(System.currentTimeMillis()));

        DocumentHeader updatedHeader = documentHeaderRepository.save(documentHeader);

        log.info("SUCCESS → Document Active Status Updated | id={} isActive={}", id, isActive);

        return updatedHeader;
    }

    //Update Document Approval Status (Pending, Reject, Approve)
    //Update Document Approval Status (Pending, Reject, Approve) — HEADER LEVEL
    @Override
    @Transactional
    public DocumentHeader updateApprovalStatus(Integer id, DocApprovalStatus status, String rejectionReason, Integer employeeId) {
        log.info("API CALL → Update Document Approval Status | id={} status={} employeeId={}",
                id, status, employeeId);

        DocumentHeader documentHeader = findDocumentHeaderById(id);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> {
                    log.error("Employee not found | employeeId={}", employeeId);
                    return new ResourceNotFoundException("Employee not found");
                });

        docHeaderStatusService.applyHeaderDecision(documentHeader, status, rejectionReason, employee.getEmail());

        DocumentHeader updatedDocumentHeader = documentHeaderRepository.save(documentHeader);

        try {
            List<DocumentDetails> details = updatedDocumentHeader.getDocumentDetails();
            if (details != null && !details.isEmpty()) {
                DocumentDetails representative = details.get(0);
                notificationService.createDocumentNotification(representative);
            } else {
                log.warn("No document details found for header {} — skipping notification", id);
            }
        } catch (Exception ex) {
            log.warn("Notification failed for header {}", id, ex);
        }

        Map<String, Object> detailsJson = new HashMap<>();
        detailsJson.put("newStatus", status.name());
        detailsJson.put("reason", rejectionReason == null ? "" : rejectionReason);
        detailsJson.put("title", updatedDocumentHeader.getTitle());
        detailsJson.put("caseId", updatedDocumentHeader.getCaseId() == null ? "" : updatedDocumentHeader.getCaseId());

        auditLogUtil.logDocumentAction(
                employee,
                "Document",
                "StatusChange",
                "Success",
                id,
                null,
                detailsJson,
                null
        );

        log.info("SUCCESS → Document Approval Status Updated | id={} status={}", id, status);

        return updatedDocumentHeader;
    }


    @Override
    public void deleteByIdDocumentHeader(Integer id) {
        log.info("API CALL → Delete Document | id={}", id);

        if (!documentHeaderRepository.existsById(id)) {
            log.error("FAILED → Delete Document | id={} reason=Document Not Found", id);
            throw new ResourceNotFoundException("DocumentHeader not found with id " + id);
        }

        documentHeaderRepository.deleteById(id);

        log.info("SUCCESS → Document Deleted | id={}", id);
    }

    // ========================================================= Document Get Operations ========================================================= //

    //Find Document By Document Id
    @Override
    public DocumentHeader findDocumentHeaderById(Integer id) {
        log.debug("Finding Document By ID | id={}", id);

        return documentHeaderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DocumentHeader", "id", id));
    }

    @Override
    public List<DocumentHeader> findDocumentHeaderByBranchId(Integer id) {
        log.info("API CALL → Find Documents By Branch ID | branchId={}", id);

        List<DocumentHeader> documents = documentHeaderRepository.findByBranch(id);

        log.debug("Found {} documents for branch ID: {}", documents.size(), id);

        return documents;
    }

    @Override
    public List<DocumentHeader> findDocumentHeaderByDepartmentId(Integer id) {
        log.info("API CALL → Find Documents By Department ID | departmentId={}", id);

        List<DocumentHeader> documents = documentHeaderRepository.findByDepartment(id);

        log.debug("Found {} documents for department ID: {}", documents.size(), id);

        return documents;
    }

    //Find All Document
    @Override
    public List<DocumentHeader> findAllDocumentHeaders() {
        log.info("API CALL → Find All Documents");

        List<DocumentHeader> documents = documentHeaderRepository.findAllDocumentHeadersOrdered();

        log.info("SUCCESS → Retrieved {} documents", documents.size());

        return documents;
    }

    @Override
    public List<DocumentHeader> findAllFilterDocumentHeaders() {
        log.info("API CALL → Find All Filtered Documents");

        Employee currentEmployee = currentUser.getCurrentEmployeeOrThrow();

        Integer branchId = null;
        Integer departmentId = null;
        Integer employeeId = null;

        if (!"ADMIN".equalsIgnoreCase(currentEmployee.getRole().getRole())) {
            branchId = currentEmployee.getBranch() != null ? currentEmployee.getBranch().getId() : null;
            departmentId = currentEmployee.getDepartment() != null ? currentEmployee.getDepartment().getId() : null;
            employeeId = currentEmployee.getId();
        }

        log.debug("Filtering documents - branchId: {}, departmentId: {}, employeeId: {}",
                branchId, departmentId, employeeId);

        List<DocumentHeader> documents = documentHeaderRepository.findFilteredDocuments(branchId, departmentId, employeeId);

        log.info("SUCCESS → Retrieved {} filtered documents", documents.size());

        return documents;
    }

    //Find All Approved Document


    //========================================== USER ==========================================

    //Find All Document For User
    @Override
    public List<DocumentHeader> findAllDocumentHeadersByEmployeeId(Integer employeeId) {
        log.info("API CALL → Find All Documents By Employee | employeeId={}", employeeId);

        List<DocumentHeader> documents = documentHeaderRepository.findAllByEmployeeId(employeeId);

        log.info("SUCCESS → Retrieved {} documents for employee ID: {}", documents.size(), employeeId);

        return documents;
    }



    @Override
    public List<DocumentHeader> getAllApprovedByEmployeeId(Integer employeeId) {
        log.info("API CALL → Get Approved Documents By Employee | employeeId={}", employeeId);
        List<DocumentHeader> headers = documentHeaderRepository.findAllApprovedByEmployeeId(employeeId);
        log.info("SUCCESS → Retrieved {} approved documents for employee ID: {}", headers.size(), employeeId);
        return headers;
    }

    @Override
    public List<DocumentHeader> getAllRejectedByEmployeeId(Integer employeeId) {
        log.info("API CALL → Get Rejected Documents By Employee | employeeId={}", employeeId);
        List<DocumentHeader> headers = documentHeaderRepository.findAllRejectedByEmployeeId(employeeId);
        log.info("SUCCESS → Retrieved {} rejected documents for employee ID: {}", headers.size(), employeeId);
        return headers;
    }

    @Override
    public List<DocumentHeader> getAllPendingByEmployeeId(Integer employeeId) {
        log.info("API CALL → Get Pending Documents By Employee | employeeId={}", employeeId);

        List<DocumentHeader> headers = documentHeaderRepository.findAllPendingByEmployeeId(employeeId);

        if (!headers.isEmpty()) {

            List<Integer> documentIds = headers.stream()
                    .map(DocumentHeader::getId)
                    .collect(Collectors.toList());

            // DocumentForwardingAuthority has no back-reference on DocumentHeader,
            // so it must be fetched separately and attached manually
            Map<Integer, DocumentForwardingAuthority> forwardingAuthorityMap =
                    forwardingAuthorityRepository.findByDocumentHeader_IdIn(documentIds)
                            .stream()
                            .collect(Collectors.toMap(
                                    fa -> fa.getDocumentHeader().getId(),
                                    fa -> fa
                            ));

            headers.forEach(doc ->
                    doc.setForwardingAuthority(forwardingAuthorityMap.get(doc.getId()))
            );
        }

        log.info("SUCCESS → Retrieved {} pending documents for employee ID: {}", headers.size(), employeeId);

        return headers;
    }

    //========================================== ADMIN ==========================================

    @Override
    public List<DocumentHeader> findAllRejectedByActionEmployeeId(Integer employeeId) {
        log.info("API CALL → Find Rejected Documents By Action Employee | employeeId={}", employeeId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> {
                    log.error("Employee not found | employeeId={}", employeeId);
                    return new IllegalArgumentException("Employee not found with ID: " + employeeId);
                });

        String role = employee.getRole().getRole();
        List<DocumentHeader> headers;

        if ("SYSTEM ADMIN".equalsIgnoreCase(role)) {
            log.debug("System Admin role - fetching all rejected documents");
            headers = documentHeaderRepository.findAllRejectedForAdmin();

        } else if ("LABORATORY ADMIN / HOD".equalsIgnoreCase(role)) {
            log.debug("Lab Admin/Director role - fetching rejected documents for branch: {}", employee.getBranch().getId());
            headers = documentHeaderRepository.findAllRejectedByBranch(employee.getBranch().getId());

        } else if ("SCIENTIFIC OFFICER".equalsIgnoreCase(role) || "CASE & EVIDENCE OFFICER".equalsIgnoreCase(role)) {
            log.debug("Officer role - fetching rejected documents for department: {}", employee.getDepartment().getId());
            headers = documentHeaderRepository.findAllRejectedByDepartment(employee.getDepartment().getId());

        } else {
            log.error("Unauthorized role for fetching rejected documents | role={}", role);
            throw new IllegalArgumentException("Unauthorized role for fetching rejected documents.");
        }

        log.info("SUCCESS → Retrieved {} rejected documents by action employee", headers.size());
        return headers;
    }
    @Override
    public List<DocumentHeader> findAllApprovedByActionEmployeeId(Integer employeeId) {
        log.info("API CALL → Find Approved Documents By Action Employee | employeeId={}", employeeId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> {
                    log.error("Employee not found | employeeId={}", employeeId);
                    return new IllegalArgumentException("Employee not found with ID: " + employeeId);
                });

        String role = employee.getRole().getRole();

        if ("SYSTEM ADMIN".equalsIgnoreCase(role)) {
            log.debug("System Admin role - fetching all approved documents");
            return documentHeaderRepository.findApprovedHeadersForAdmin();

        } else if ("LABORATORY ADMIN / HOD".equalsIgnoreCase(role)) {
            log.debug("Lab Admin/Director role - fetching approved documents for branch: {}", employee.getBranch().getId());
            return documentHeaderRepository.findApprovedHeadersByBranch(employee.getBranch());

        } else if ("SCIENTIFIC OFFICER".equalsIgnoreCase(role) || "CASE & EVIDENCE OFFICER".equalsIgnoreCase(role)) {
            log.debug("Officer role - fetching approved documents for department: {}", employee.getDepartment().getId());
            return documentHeaderRepository.findApprovedHeadersByDepartment(employee.getDepartment());
        }

        log.error("Unauthorized role for fetching approved documents | role={}", role);
        throw new IllegalArgumentException("Unauthorized role for fetching approved documents.");
    }
    @Override
    public List<DocumentHeader> findAllTrashApprovedByEmployeeId(Integer employeeId) {
        log.info("API CALL → Find Trash Approved Documents By Employee | employeeId={}", employeeId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> {
                    log.error("Employee not found | employeeId={}", employeeId);
                    return new IllegalArgumentException("Employee not found with ID: " + employeeId);
                });

        String role = employee.getRole().getRole();

        if ("SYSTEM ADMIN".equalsIgnoreCase(role)) {
            log.debug("System Admin role - fetching all trash documents");
            return documentHeaderRepository.findDeletedDetailsForAdmin();

        } else if ("LABORATORY ADMIN / HOD".equalsIgnoreCase(role)) {
            log.debug("Lab Admin/Director role - fetching trash documents for branch: {}", employee.getBranch().getId());
            return documentHeaderRepository.findDeletedDetailsByBranch(employee.getBranch());

        } else if ("SCIENTIFIC OFFICER".equalsIgnoreCase(role) || "CASE & EVIDENCE OFFICER".equalsIgnoreCase(role)) {
            log.debug("Officer role - fetching trash documents for department: {}", employee.getDepartment().getId());
            return documentHeaderRepository.findDeletedDetailsByDepartment(employee.getDepartment());
        }

        log.error("Unauthorized role for fetching trash documents | role={}", role);
        throw new IllegalArgumentException("Unauthorized role for fetching trash documents.");
    }
    // ========================================================= Document Count Operations ========================================================= //

    //Count All Approved Document
    @Override
    public long countApprovedDocuments() {
        log.info("API CALL → Count All Approved Documents");
        long count = documentHeaderRepository.countByApprovalStatusIn(List.of(DocApprovalStatus.APPROVED));
        log.info("SUCCESS → Counted {} approved documents", count);
        return count;
    }

    @Override
    public long countRejectedDocuments() {
        log.info("API CALL → Count All Rejected Documents");
        long count = documentHeaderRepository.countByApprovalStatusIn(List.of(DocApprovalStatus.REJECTED));
        log.info("SUCCESS → Counted {} rejected documents", count);
        return count;
    }

    @Override
    public long countPendingDocuments() {
        log.info("API CALL → Count All Pending Documents");
        long count = documentHeaderRepository.countByApprovalStatusIn(List.of(DocApprovalStatus.PENDING));
        log.info("SUCCESS → Counted {} pending documents", count);
        return count;
    }





    @Override
    public List<DocumentHeader> getAllApproved() {
        log.info("API CALL → Get All Approved Documents");
        List<DocumentHeader> documents = documentHeaderRepository.findAllByApprovalStatusesOrdered(
                List.of(DocApprovalStatus.APPROVED.name()));
        log.info("SUCCESS → Retrieved {} approved documents", documents.size());
        return documents;
    }

    @Override
    public List<DocumentHeader> getAllRejected() {
        log.info("API CALL → Get All Rejected Documents");
        List<DocumentHeader> documents = documentHeaderRepository.findAllByApprovalStatusesOrdered(
                List.of(DocApprovalStatus.REJECTED.name()));
        log.info("SUCCESS → Retrieved {} rejected documents", documents.size());
        return documents;
    }

    @Override
    public List<DocumentHeader> getAllPending() {
        log.info("API CALL → Get All Pending Documents");
        try {
            List<DocumentHeader> documents = documentHeaderRepository.findAllByApprovalStatusesOrdered(
                    List.of(DocApprovalStatus.PENDING.name()));
            log.info("SUCCESS → Retrieved {} pending documents", documents.size());
            return documents;
        } catch (Exception e) {
            log.error("FAILED → Get All Pending Documents | reason={}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch pending documents", e);
        }
    }

    //Count All Reject Document


    //========================================== USER ==========================================

    //Count All Document By Employee ID (User)
    @Override
    public long countDocumentHeadersByEmployeeId(Integer employeeId) {
        log.info("API CALL → Count Documents By Employee | employeeId={}", employeeId);

        long count = documentHeaderRepository.countByEmployeeId(employeeId);

        log.info("SUCCESS → Counted {} documents for employee ID: {}", count, employeeId);

        return count;
    }

    //Count All Approve Document By Employee ID (User)
    @Override
    public long countApprovedDocumentsByEmployeeId(Integer employeeId) {
        log.info("API CALL → Count Approved Documents By Employee | employeeId={}", employeeId);

        long count = documentHeaderRepository.countByApprovalStatusAndEmployeeId(DocApprovalStatus.APPROVED, employeeId);

        log.info("SUCCESS → Counted {} approved documents for employee ID: {}", count, employeeId);

        return count;
    }

    //Count All Reject Document By Employee ID (User)
    @Override
    public long countRejectedDocumentsByEmployeeId(Integer employeeId) {
        log.info("API CALL → Count Rejected Documents By Employee | employeeId={}", employeeId);

        long count = documentHeaderRepository.countByApprovalStatusAndEmployeeId(DocApprovalStatus.REJECTED, employeeId);

        log.info("SUCCESS → Counted {} rejected documents for employee ID: {}", count, employeeId);

        return count;
    }

    //Count All Pending Document By Employee ID (User)
    @Override
    public long countPendingDocumentsByEmployeeId(Integer employeeId) {
        log.info("API CALL → Count Pending Documents By Employee | employeeId={}", employeeId);

        long count = documentHeaderRepository.countByApprovalStatusAndEmployeeId(DocApprovalStatus.PENDING, employeeId);

        log.info("SUCCESS → Counted {} pending documents for employee ID: {}", count, employeeId);

        return count;
    }

    //========================================== ADMIN ==========================================

    @Override
    public long countRejectedByActionEmployeeId(Integer employeeId) {
        log.info("API CALL → Count Rejected By Action Employee | employeeId={}", employeeId);

        long count = documentHeaderRepository.countByEmployeeByAndApprovalStatus(employeeId, DocApprovalStatus.REJECTED);

        log.info("SUCCESS → Counted {} rejected documents by action employee", count);

        return count;
    }

    @Override
    public long countApprovedByActionEmployeeId(Integer employeeId) {
        log.info("API CALL → Count Approved By Action Employee | employeeId={}", employeeId);

        long count = documentHeaderRepository.countByEmployeeByAndApprovalStatus(employeeId, DocApprovalStatus.APPROVED);

        log.info("SUCCESS → Counted {} approved documents by action employee", count);

        return count;
    }

    // =================================================== Document Count For Graph Operations =================================================== //

    @Override
    public Map<String, Object> countAllDocumentsByIdWithMonth(Integer employeeId, Timestamp startDate, Timestamp endDate) {
        log.info("API CALL → Count Documents By ID With Month | employeeId={} startDate={} endDate={}",
                employeeId, startDate, endDate);

        List<DocumentHeader> approvedDocuments = documentHeaderRepository.findAllByEmployeeIdAndApprovalStatusAndUpdatedOnBetween(
                employeeId, DocApprovalStatus.APPROVED, startDate, endDate);

        List<DocumentHeader> rejectedDocuments = documentHeaderRepository.findAllByEmployeeIdAndApprovalStatusAndUpdatedOnBetween(
                employeeId, DocApprovalStatus.REJECTED, startDate, endDate);

        Map<String, Object> approvedDocumentsMap = groupDocumentsByMonth(approvedDocuments);
        Map<String, Object> rejectedDocumentsMap = groupDocumentsByMonth(rejectedDocuments);

        Map<String, Object> response = new HashMap<>();
        response.put("approvedDocuments", approvedDocumentsMap.get("totalDocumentsByMonth"));
        response.put("rejectedDocuments", rejectedDocumentsMap.get("totalDocumentsByMonth"));
        response.put("months", approvedDocumentsMap.get("months")); // both maps will have the same months list

        log.info("SUCCESS → Generated document count summary for employee ID: {}", employeeId);

        return response;
    }

    private Map<String, Object> groupDocumentsByMonth(List<DocumentHeader> documents) {
        // List of all 12 months in order
        List<String> allMonths = Arrays.asList("JAN", "FEB", "MAR", "APR", "MAY", "JUN",
                "JUL", "AUG", "SEP", "OCT", "NOV", "DEC");

        // Count documents grouped by month
        Map<String, Long> monthCountMap = documents.stream()
                .collect(Collectors.groupingBy(doc -> {
                    LocalDateTime updatedOn = doc.getUpdatedOn().toLocalDateTime();
                    return updatedOn.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toUpperCase(); // ensure uppercase
                }, Collectors.counting()));

        // Prepare result lists
        List<Long> totalDocumentsByMonth = new ArrayList<>();

        // Ensure all months are present with their counts (or zero if absent)
        for (String month : allMonths) {
            totalDocumentsByMonth.add(monthCountMap.getOrDefault(month, 0L));
        }

        Map<String, Object> groupedResult = new HashMap<>();
        groupedResult.put("months", allMonths); // all 12 months
        groupedResult.put("totalDocumentsByMonth", totalDocumentsByMonth);

        return groupedResult;
    }

    @Override
    public List<DocumentHeader> getPendingDocumentsByBranch(Integer branchId) {
        log.info("API CALL → Get Pending Documents By Branch | branchId={}", branchId);
        List<DocumentHeader> headers = documentHeaderRepository.findPendingByBranch(branchId);
        log.info("SUCCESS → Retrieved {} pending documents for branch ID: {}", headers.size(), branchId);
        return headers;
    }

    @Override
    public List<DocumentHeader> getPendingDocumentsByDepartment(Integer departmentId) {
        log.info("API CALL → Get Pending Documents By Department | departmentId={}", departmentId);
        List<DocumentHeader> headers = documentHeaderRepository.findPendingByDepartment(departmentId);
        log.info("SUCCESS → Retrieved {} pending documents for department ID: {}", headers.size(), departmentId);
        return headers;
    }

    @Override
    public List<DocumentHeader> getAllPendingDocuments() {
        log.info("API CALL → Get All Pending Documents");
        List<DocumentHeader> headers = documentHeaderRepository.findAllPendingForAdmin();
        log.info("SUCCESS → Retrieved {} pending documents", headers.size());
        return headers;
    }
    private void filterPendingDetails(List<DocumentHeader> headers) {
        headers.forEach(header -> {
            if (header.getDocumentDetails() != null) {
                header.setDocumentDetails(
                        header.getDocumentDetails().stream()
                                .filter(d ->
                                        d.getStatus() == DocApprovalStatus.PENDING &&
                                                Boolean.FALSE.equals(d.getIsDeleted())
                                )
                                .toList()
                );
            }
        });
    }

    @Override
    public List<DocumentHeader> findApprovedDocumentsByBranchAdmin(Integer employeeId) {
        log.info("API CALL → Find Approved Documents By Branch Admin | employeeId={}", employeeId);

        // Fetch documents approved by the branch admin
        DocApprovalStatus approvalStatus = DocApprovalStatus.APPROVED;
        List<DocumentHeader> documents = documentHeaderRepository.findByEmployee_IdAndApprovalStatus(employeeId, approvalStatus);

        log.info("SUCCESS → Retrieved {} approved documents by branch admin", documents.size());

        return documents;
    }

//    @Override
//    public List<DocumentHeader> getAllPendingDocuments() {
//        log.info("API CALL → Get All Pending Documents");
//
//        List<DocumentHeader> headers =
//                documentHeaderRepository.findAllPendingForAdmin();
//
//        filterPendingDetails(headers);
//
//        log.info("SUCCESS → Retrieved {} pending documents", headers.size());
//
//        return headers;
//    }

    @Override
    public Long countDocumentHeadersByBranchId(Integer branch) {
        log.info("API CALL → Count Documents By Branch | branchId={}", branch);

        Long count = documentHeaderRepository.countByEmployee_BranchId(branch);

        log.info("SUCCESS → Counted {} documents for branch ID: {}", count, branch);

        return count;
    }

    @Override
    public Long countPendingDocumentsByBranchId(Integer branch) {
        log.info("API CALL → Count Pending Documents By Branch | branchId={}", branch);

        Long count = documentHeaderRepository.countByEmployee_BranchIdAndApprovalStatusIn(branch, List.of(DocApprovalStatus.PENDING,DocApprovalStatus.PARTIALLY_PENDING));

        log.info("SUCCESS → Counted {} pending documents for branch ID: {}", count, branch);

        return count;
    }

    @Override
    public Long countApprovedByBranchId(Integer branch) {
        log.info("API CALL → Count Approved Documents By Branch | branchId={}", branch);

        Long count = documentHeaderRepository.countByEmployee_BranchIdAndApprovalStatusIn(branch, List.of(DocApprovalStatus.APPROVED,DocApprovalStatus.PARTIALLY_APPROVED));

        log.info("SUCCESS → Counted {} approved documents for branch ID: {}", count, branch);

        return count;
    }

    @Override
    public Long countRejectedByBranchId(Integer branch) {
        log.info("API CALL → Count Rejected Documents By Branch | branchId={}", branch);

        Long count = documentHeaderRepository.countByEmployee_BranchIdAndApprovalStatusIn(branch, List.of(DocApprovalStatus.REJECTED,DocApprovalStatus.PARTIALLY_REJECT));

        log.info("SUCCESS → Counted {} rejected documents for branch ID: {}", count, branch);

        return count;
    }

    @Override
    public List<DocumentHeader> searchDocuments(SearchCriteria criteria) {
        log.info("API CALL → Search Documents | criteria={}", criteria);

        // 🔹 No metadata → normal search
        if (criteria.getMetadata() == null || criteria.getMetadata().isEmpty()) {
            log.debug("Searching without metadata filters");
            List<DocumentHeader> results = documentHeaderRepository.searchDocuments(
                    criteria.getFileNo(),
                    criteria.getTitle(),
                    criteria.getSubject(),
                    criteria.getVersion(),
                    criteria.getCategoryId(),
                    criteria.getBranchId(),
                    criteria.getDepartmentId(),
                    null,
                    null
            );

            log.info("SUCCESS → Found {} documents without metadata filters", results.size());
            return results;
        }

        log.debug("Searching with {} metadata filters", criteria.getMetadata().size());

        // 🔹 Metadata present → AND logic
        List<DocumentHeader> result = null;

        for (DocumentSaveRequest.MetadataRequest meta : criteria.getMetadata()) {

            List<DocumentHeader> temp = documentHeaderRepository.searchDocuments(
                    criteria.getFileNo(),
                    criteria.getTitle(),
                    criteria.getSubject(),
                    criteria.getVersion(),
                    criteria.getCategoryId(),
                    criteria.getBranchId(),
                    criteria.getDepartmentId(),
                    meta.getKey(),
                    meta.getValue()
            );

            // First metadata filter
            if (result == null) {
                result = temp;
            } else {
                Set<Integer> ids = temp.stream()
                        .map(DocumentHeader::getId)
                        .collect(Collectors.toSet());

                result = result.stream()
                        .filter(d -> ids.contains(d.getId()))
                        .toList();
            }
        }

        log.info("SUCCESS → Found {} documents with metadata filters", result != null ? result.size() : 0);

        return result != null ? result : List.of();
    }

//    @Override
//    public List<DocumentHeader> getPendingDocumentsByDepartment(Integer departmentId) {
//        log.info("API CALL → Get Pending Documents By Department | departmentId={}", departmentId);
//
//        List<DocumentHeader> headers =
//                documentHeaderRepository.findPendingByDepartment(departmentId);
//
//        filterPendingDetails(headers);
//
//        log.info("SUCCESS → Retrieved {} pending documents for department ID: {}", headers.size(), departmentId);
//
//        return headers;
//    }

    @Override
    public List<DocumentResponse> getFilteredDocuments(Integer categoryId, DocApprovalStatus approvalStatus,
                                                       Timestamp startDate, Timestamp endDate,
                                                       Integer branchId, Integer departmentId) {
        log.info("API CALL → Get Filtered Documents | categoryId={} approvalStatus={} branchId={} departmentId={}",
                categoryId, approvalStatus, branchId, departmentId);

        List<DocumentResponse> results = documentHeaderRepository.findFilteredDocuments(categoryId, approvalStatus, startDate, endDate, branchId, departmentId);

        log.info("SUCCESS → Found {} filtered documents", results.size());

        return results;
    }

    @Override
    public List<DocumentResponse> getFilteredDocumentsById(Integer categoryId, DocApprovalStatus approvalStatus,
                                                           Timestamp startDate, Timestamp endDate,
                                                           Integer branchId, Integer departmentId, Integer employeeId) {
        log.info("API CALL → Get Filtered Documents By ID | categoryId={} approvalStatus={} branchId={} departmentId={} employeeId={}",
                categoryId, approvalStatus, branchId, departmentId, employeeId);

        List<DocumentResponse> results = documentHeaderRepository.findFilteredDocumentsById(categoryId, approvalStatus, startDate, endDate, branchId, departmentId, employeeId);

        log.info("SUCCESS → Found {} filtered documents by ID", results.size());

        return results;
    }

    @Override
    public long countDocumentHeadersByDepartmentId(Integer departmentId) {
        log.info("API CALL → Count Documents By Department | departmentId={}", departmentId);

        long count = documentHeaderRepository.countByEmployee_Department_Id(departmentId);

        log.info("SUCCESS → Counted {} documents for department ID: {}", count, departmentId);

        return count;
    }

    @Override
    public long countPendingDocumentsByDepartmentId(Integer departmentId) {
        log.info("API CALL → Count Pending Documents By Department | departmentId={}", departmentId);

        long count = documentHeaderRepository.countByEmployee_Department_IdAndApprovalStatusIn(departmentId, List.of(DocApprovalStatus.PENDING,DocApprovalStatus.PARTIALLY_PENDING));

        log.info("SUCCESS → Counted {} pending documents for department ID: {}", count, departmentId);

        return count;
    }

    @Override
    public long countApprovedByDepartmentId(Integer departmentId) {
        log.info("API CALL → Count Approved Documents By Department | departmentId={}", departmentId);

        long count = documentHeaderRepository.countByEmployee_Department_IdAndApprovalStatusIn(departmentId, List.of(DocApprovalStatus.APPROVED,DocApprovalStatus.PARTIALLY_APPROVED));

        log.info("SUCCESS → Counted {} approved documents for department ID: {}", count, departmentId);

        return count;
    }

    @Override
    public long countRejectedByDepartmentId(Integer departmentId) {
        log.info("API CALL → Count Rejected Documents By Department | departmentId={}", departmentId);

        long count = documentHeaderRepository.countByEmployee_Department_IdAndApprovalStatusIn(departmentId, List.of(DocApprovalStatus.REJECTED,DocApprovalStatus.PARTIALLY_REJECT));

        log.info("SUCCESS → Counted {} rejected documents for department ID: {}", count, departmentId);

        return count;
    }

    @Override
    public Map<String, Object> getApprovalSummaryByEmployeeId(Integer employeeId, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("API CALL → Get Approval Summary By Employee | employeeId={} startDate={} endDate={}",
                employeeId, startDate, endDate);

        // Fetch counts grouped by month and status
        List<Object[]> results = documentHeaderRepository.countByEmployeeGroupedByStatusAndMonth(employeeId, startDate, endDate);

        // Initialize month names
        String[] months = {"JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"};

        // Initialize document counts for each status (12 months each)
        int[] pendingDocuments = new int[12];
        int[] rejectedDocuments = new int[12];
        int[] approvedDocuments = new int[12];

        // Populate the counts based on query results
        for (Object[] row : results) {
            Integer month = (Integer) row[0]; // Month (1-12)
            DocApprovalStatus status = (DocApprovalStatus) row[1]; // Approval status
            Long count = (Long) row[2]; // Document count

            int index = month - 1; // Convert month to 0-based index
            switch (status) {
                case PENDING -> pendingDocuments[index] = count.intValue();
                case REJECTED -> rejectedDocuments[index] = count.intValue();
                case APPROVED -> approvedDocuments[index] = count.intValue();
            }
        }

        // Construct the response map
        Map<String, Object> response = new HashMap<>();
        response.put("months", months);
        response.put("pendingDocuments", pendingDocuments);
        response.put("rejectedDocuments", rejectedDocuments);
        response.put("approvedDocuments", approvedDocuments);

        log.info("SUCCESS → Generated approval summary for employee ID: {}", employeeId);

        return response;
    }

    @Override
    public Map<String, Object> getMonthlyApprovalSummary(String queryType, Integer departmentOrBranchId, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("API CALL → Get Monthly Approval Summary | queryType={} id={} startDate={} endDate={}",
                queryType, departmentOrBranchId, startDate, endDate);

        String[] months = {"JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"};
        int[] pendingDocuments = new int[12];
        int[] rejectedDocuments = new int[12];
        int[] approvedDocuments = new int[12];

        // Choose query based on the type
        List<Object[]> results;
        if ("department".equals(queryType)) {
            log.debug("Getting department summary for department ID: {}", departmentOrBranchId);
            results = documentHeaderRepository.countByDepartment(departmentOrBranchId, startDate, endDate);
        } else if ("branch".equals(queryType)) {
            log.debug("Getting branch summary for branch ID: {}", departmentOrBranchId);
            results = documentHeaderRepository.countByBranch(departmentOrBranchId, startDate, endDate);
        } else {
            log.error("Invalid query type: {}", queryType);
            throw new IllegalArgumentException("Invalid query type.");
        }

        // Populate counts from query results
        for (Object[] row : results) {
            Integer month = (Integer) row[0]; // 1-12
            DocApprovalStatus status = (DocApprovalStatus) row[1]; // Cast to the enum
            Long count = (Long) row[2];

            int index = month - 1; // Convert to 0-based index
            switch (status) {
                case PENDING -> pendingDocuments[index] = count.intValue();
                case REJECTED -> rejectedDocuments[index] = count.intValue();
                case APPROVED -> approvedDocuments[index] = count.intValue();
            }
        }

        // Construct the JSON response
        Map<String, Object> response = new HashMap<>();
        response.put("months", months);
        response.put("pendingDocuments", pendingDocuments);
        response.put("rejectedDocuments", rejectedDocuments);
        response.put("approvedDocuments", approvedDocuments);

        log.info("SUCCESS → Generated monthly approval summary");

        return response;
    }

    @Override
    public Map<String, Object> getTotalMonthlySummary(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("API CALL → Get Total Monthly Summary | startDate={} endDate={}", startDate, endDate);

        // Month names for the response
        String[] months = {"JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"};
        int[] pendingDocuments = new int[12];
        int[] rejectedDocuments = new int[12];
        int[] approvedDocuments = new int[12];

        // Fetch results from repository
        List<Object[]> results = documentHeaderRepository.countTotalByMonth(startDate, endDate);

        // Populate counts from query results
        for (Object[] row : results) {
            int month = ((Number) row[0]).intValue(); // Extract month (1-based index)
            String status = ((DocApprovalStatus) row[1]).name();
            long count = ((Number) row[2]).longValue(); // Extract count

            int index = month - 1; // Convert to 0-based index
            switch (status) {
                case "PENDING" -> pendingDocuments[index] = (int) count;
                case "REJECTED" -> rejectedDocuments[index] = (int) count;
                case "APPROVED" -> approvedDocuments[index] = (int) count;
            }
        }

        // Construct response map
        Map<String, Object> response = new HashMap<>();
        response.put("months", months);
        response.put("pendingDocuments", pendingDocuments);
        response.put("rejectedDocuments", rejectedDocuments);
        response.put("approvedDocuments", approvedDocuments);

        log.info("SUCCESS → Generated total monthly summary");

        return response;
    }

    @Override
    public Map<String, Object> getTotalSummaryByTopBranches(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("API CALL → Get Total Summary By Top Branches | startDate={} endDate={}", startDate, endDate);

        // Step 1: Fetch top 10 branches
        List<Object[]> topBranchResults = documentHeaderRepository.findTopBranchesByCount(startDate, endDate);
        List<String> topBranches = topBranchResults.stream()
                .limit(10)
                .map(row -> (String) row[0])
                .toList();

        log.debug("Found top {} branches", topBranches.size());

        // Step 2: Prepare status-wise maps with default 0s
        Map<String, Integer> pendingMap = new LinkedHashMap<>();
        Map<String, Integer> approvedMap = new LinkedHashMap<>();
        Map<String, Integer> rejectedMap = new LinkedHashMap<>();

        for (String branch : topBranches) {
            pendingMap.put(branch, 0);
            approvedMap.put(branch, 0);
            rejectedMap.put(branch, 0);
        }

        // Step 3: Fetch all status counts
        List<Object[]> results = documentHeaderRepository.countStatusByBranch(startDate, endDate);

        for (Object[] row : results) {
            String branchName = (String) row[0];
            String status = ((DocApprovalStatus) row[1]).name();
            long count = ((Number) row[2]).longValue();

            if (!topBranches.contains(branchName)) continue;

            switch (status) {
                case "PENDING" -> pendingMap.put(branchName, (int) count);
                case "APPROVED" -> approvedMap.put(branchName, (int) count);
                case "REJECTED" -> rejectedMap.put(branchName, (int) count);
            }
        }

        // Step 4: Construct final response
        Map<String, Object> response = new HashMap<>();
        response.put("branches", topBranches);
        response.put("pendingDocuments", topBranches.stream().map(pendingMap::get).toList());
        response.put("approvedDocuments", topBranches.stream().map(approvedMap::get).toList());
        response.put("rejectedDocuments", topBranches.stream().map(rejectedMap::get).toList());

        log.info("SUCCESS → Generated top branches summary");

        return response;
    }

    @Override
    public void exportDocuments(OutputStream outputStream, DocFilterRequest filterRequest) throws Exception {
        log.info("API CALL → Export Documents | docType={} categoryId={} branchId={} departmentId={}",
                filterRequest.getDocType(), filterRequest.getCategoryId(), filterRequest.getBranchId(), filterRequest.getDepartmentId());

        // Filter documents based on the request
        List<DocumentResponse> filteredDocuments = getFilteredDocuments(filterRequest);

        // Fetch dynamic names for the file
        String branchName = branchMasterRepository.findNameById(filterRequest.getBranchId()).orElse("AllBranch");
        String departmentName = departmentMasterRepository.findNameById(filterRequest.getDepartmentId()).orElse("AllDepartment");
        String categoryName = categoryMasterRepository.findNameById(filterRequest.getCategoryId()).orElse("AllCategory");

        // Prepare the dynamic file name
        String fileName;
        if ("PDF".equalsIgnoreCase(filterRequest.getDocType())) {
            fileName = String.format("%s_%s_%s.pdf", branchName, departmentName, categoryName);
            log.debug("Generating PDF with fileName: {}", fileName);
            pdfGenerator.generate(outputStream, filteredDocuments, filterRequest, fileName);
        } else if ("EXCEL".equalsIgnoreCase(filterRequest.getDocType())) {
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
            log.debug("Generating Excel with fileName: {}", fileName);
            excelGenerator.generateExcel(outputStream, filteredDocuments, filterRequest, fileName);
        } else {
            log.error("Unsupported document type: {}", filterRequest.getDocType());
            throw new IllegalArgumentException("Unsupported document type: " + filterRequest.getDocType());
        }

        log.info("SUCCESS → Documents exported | fileName={} documentCount={}", fileName, filteredDocuments.size());
    }

    private List<DocumentResponse> getFilteredDocuments(DocFilterRequest filterRequest) {
        return documentHeaderRepository.findFilteredDocuments(
                filterRequest.getCategoryId(),
                filterRequest.getApprovalStatus(),
                filterRequest.getStartDate(),
                filterRequest.getEndDate(),
                filterRequest.getBranchId(),
                filterRequest.getDepartmentId()
        );
    }

    private List<DocumentResponse> getFilteredDocumentsById(DocFilterRequest filterRequest) {
        return documentHeaderRepository.findFilteredDocumentsById(
                filterRequest.getCategoryId(),
                filterRequest.getApprovalStatus(),
                filterRequest.getStartDate(),
                filterRequest.getEndDate(),
                filterRequest.getBranchId(),
                filterRequest.getDepartmentId(),
                filterRequest.getEmployeeId()
        );
    }

    @Override
    public void exportDocumentsById(OutputStream outputStream, DocFilterRequest filterRequest) throws Exception {
        log.info("API CALL → Export Documents By ID | docType={} categoryId={} branchId={} departmentId={} employeeId={}",
                filterRequest.getDocType(), filterRequest.getCategoryId(), filterRequest.getBranchId(),
                filterRequest.getDepartmentId(), filterRequest.getEmployeeId());

        // Filter documents based on the request
        List<DocumentResponse> filteredDocuments = getFilteredDocumentsById(filterRequest);

        // Fetch dynamic names for the file
        String branchName = branchMasterRepository.findNameById(filterRequest.getBranchId()).orElse("AllBranch");
        String departmentName = departmentMasterRepository.findNameById(filterRequest.getDepartmentId()).orElse("AllDepartment");
        String categoryName = categoryMasterRepository.findNameById(filterRequest.getCategoryId()).orElse("AllCategory");

        // Prepare the dynamic file name
        String fileName;
        if ("PDF".equalsIgnoreCase(filterRequest.getDocType())) {
            fileName = String.format("%s_%s_%s.pdf", branchName, departmentName, categoryName);
            log.debug("Generating PDF by ID with fileName: {}", fileName);
            pdfGenerator.generate(outputStream, filteredDocuments, filterRequest, fileName);
        } else if ("EXCEL".equalsIgnoreCase(filterRequest.getDocType())) {
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
            log.debug("Generating Excel by ID with fileName: {}", fileName);
            excelGenerator.generateExcel(outputStream, filteredDocuments, filterRequest, fileName);
        } else {
            log.error("Unsupported document type: {}", filterRequest.getDocType());
            throw new IllegalArgumentException("Unsupported document type: " + filterRequest.getDocType());
        }

        log.info("SUCCESS → Documents exported by ID | fileName={} documentCount={}", fileName, filteredDocuments.size());
    }

    @Override
    public DocumentResponse2 getDocumentsByFileNo(String fileNo) {
        log.info("API CALL → Get Documents By File No | fileNo={}", fileNo);

        DocumentHeader header = documentHeaderRepository.findByFileNo(fileNo)
                .orElseThrow(() -> {
                    log.error("Document not found with fileNo: {}", fileNo);
                    return new RuntimeException("Document not found with fileNo: " + fileNo);
                });

        List<DocumentResponse2.FileInfo> fileInfos = header.getDocumentDetails().stream()
                .map(d -> new DocumentResponse2.FileInfo(d.getDocName(), d.getVersion(), d.getPath(), d.getId()))
                .collect(Collectors.toList());

        log.info("SUCCESS → Retrieved documents for fileNo: {} with {} files", fileNo, fileInfos.size());

        return new DocumentResponse2(
                header.getFileNo(),
                header.getTitle(),
                header.getSubject(),
                fileInfos
        );
    }

    @Override
    public ApiResponse<DocumentHeader> findProjectByDocName(String docName) {
        log.info("API CALL → Find Project By Document Name | docName={}", docName);

        DocumentDetails documentDetails = documentDetailsRepository.findByDocName(docName);

        if (documentDetails == null) {
            log.warn("Document not found with name: {}", docName);
            return ResponseUtils.createNotFoundResponse("Document not found", HttpStatus.NOT_FOUND.value());
        }

        Integer docId = documentDetails.getDocumentHeader().getId();
        Optional<DocumentHeader> documentHeaderOptional = documentHeaderRepository.findById(docId);

        if (documentHeaderOptional.isPresent()) {
            // Return DocumentHeader instead of DocumentDetails
            log.info("SUCCESS → Found document header for docName: {}", docName);
            return ResponseUtils.createSuccessResponse(documentHeaderOptional.get(), new TypeReference<DocumentHeader>() {});
        } else {
            log.warn("Document header not found for document name: {}", docName);
            return ResponseUtils.createNotFoundResponse("Document Header not found for the given Document Name", HttpStatus.NOT_FOUND.value());
        }
    }

    @Override
    public List<DuplicateDocumentResponse> getDuplicateDocuments() {
        log.info("API CALL → Get Duplicate Documents");

        // Find all documents marked as duplicates
        List<DocumentDetails> duplicateDocuments = documentDetailsRepository.findByIsDuplicateTrue();

        log.debug("Found {} duplicate documents", duplicateDocuments.size());

        // Group duplicates by their original document
        Map<Integer, List<DocumentDetails>> duplicatesByOriginal = duplicateDocuments.stream()
                .filter(doc -> doc.getDocumentId() != null) // Ensure it has original reference
                .collect(Collectors.groupingBy(doc -> doc.getDocumentId().getId()));

        List<DuplicateDocumentResponse> response = new ArrayList<>();

        for (Map.Entry<Integer, List<DocumentDetails>> entry : duplicatesByOriginal.entrySet()) {
            Integer originalId = entry.getKey();
            List<DocumentDetails> duplicates = entry.getValue();

            // Get original document
            DocumentDetails originalDoc = documentDetailsRepository.findById(originalId)
                    .orElse(null);

            if (originalDoc != null) {
                DuplicateDocumentResponse duplicateResponse = new DuplicateDocumentResponse();
                duplicateResponse.setOriginalDocumentId(originalId);
                duplicateResponse.setOriginalFileName(originalDoc.getDocName());
                duplicateResponse.setOriginalFilePath(originalDoc.getPath());

                // Map duplicate files
                List<DuplicateDocumentResponse.DuplicateFileInfo> duplicateInfos = duplicates.stream()
                        .map(this::mapToDuplicateFileInfo)
                        .collect(Collectors.toList());

                duplicateResponse.setDuplicateFiles(duplicateInfos);
                response.add(duplicateResponse);
            }
        }

        log.info("SUCCESS → Retrieved {} duplicate document groups", response.size());

        return response;
    }

    private DuplicateDocumentResponse.DuplicateFileInfo mapToDuplicateFileInfo(DocumentDetails doc) {
        DuplicateDocumentResponse.DuplicateFileInfo info = new DuplicateDocumentResponse.DuplicateFileInfo();
        info.setDuplicateId(doc.getId());
        info.setDuplicateFileName(doc.getDocName());
        info.setDuplicateFilePath(doc.getPath());
        info.setVersion(doc.getVersion());
        info.setCreatedOn(doc.getCreatedOn());
        return info;
    }

    @Override
    @Transactional
    public ApiResponse<MessageResponse> deleteDuplicateFile(Integer duplicateId, HttpServletRequest request) {
        log.info("API CALL → Delete Duplicate File | duplicateId={}", duplicateId);

        MessageResponse msg = new MessageResponse();
        Employee empObj = currentUser.getCurrentEmployeeOrThrow();

        try {
            // Find the duplicate document
            DocumentDetails duplicateDoc = documentDetailsRepository.findById(duplicateId)
                    .orElseThrow(() -> {
                        log.error("Duplicate document not found | duplicateId={}", duplicateId);
                        return new ResourceNotFoundException("Duplicate document not found with id: " + duplicateId);
                    });

            // Verify it's actually marked as duplicate
            if (!duplicateDoc.getIsDuplicate()) {
                log.warn("Document is not marked as duplicate | duplicateId={}", duplicateId);
                msg.setMsg("Document is not marked as a duplicate");
                return ResponseUtils.createFailureResponse(
                        msg,
                        new TypeReference<>() {},
                        "Document is not marked as duplicate",
                        HttpStatus.BAD_REQUEST.value()
                );
            }

            // Get file path
            String filePath = duplicateDoc.getPath();
            Path physicalFilePath = Paths.get(documentStoragePath, filePath);

            // Log audit before deletion
            Map<String, Object> detailsJson = new HashMap<>();
            detailsJson.put("duplicateId", duplicateDoc.getId());
            detailsJson.put("fileName", duplicateDoc.getDocName());
            detailsJson.put("originalDocumentId", duplicateDoc.getDocumentId() != null ? duplicateDoc.getDocumentId().getId() : null);

            // Delete physical file
            boolean physicalFileDeleted = false;
            if (Files.exists(physicalFilePath)) {
                try {
                    Files.delete(physicalFilePath);
                    physicalFileDeleted = true;
                    log.info("Deleted physical file: {}", physicalFilePath);
                } catch (IOException e) {
                    log.error("Failed to delete physical file: {}", physicalFilePath, e);
                    // Continue with DB deletion even if physical file deletion fails
                }
            }

            // Delete from database
            documentDetailsRepository.delete(duplicateDoc);

            // Update original document if needed (remove reference)
            if (duplicateDoc.getDocumentId() != null) {
                // Check if any duplicates remain for this original
                List<DocumentDetails> remainingDuplicates = documentDetailsRepository
                        .findByDocumentIdAndIsDuplicateTrue(duplicateDoc.getDocumentId());
                if (remainingDuplicates.isEmpty()) {
                    // No duplicates left, update the original if needed
                    DocumentDetails original = documentDetailsRepository.findById(duplicateDoc.getDocumentId().getId())
                            .orElse(null);
                    if (original != null) {
                        // You can update original document properties here if needed
                        // For example: original.setHasDuplicates(false);
                    }
                }
            }

            msg.setMsg("Duplicate file deleted successfully");

            // Audit log
            auditLogUtil.logDocumentAction(
                    empObj,
                    "DuplicateDocument",
                    "Delete",
                    "Success",
                    duplicateDoc.getDocumentId() != null ? duplicateDoc.getDocumentId().getId() : null,
                    null,
                    detailsJson,
                    request
            );

            log.info("SUCCESS → Duplicate file deleted | duplicateId={}", duplicateId);

            return ResponseUtils.createSuccessResponse(msg, new TypeReference<>() {});

        } catch (ResourceNotFoundException e) {
            log.error("FAILED → Delete Duplicate File | duplicateId={} reason=Not Found", duplicateId);
            msg.setMsg("Duplicate document not found");

            auditLogUtil.logDocumentAction(
                    empObj,
                    "DuplicateDocument",
                    "Delete",
                    "Failure",
                    null,
                    null,
                    Map.of("error", e.getMessage(), "duplicateId", duplicateId),
                    request
            );
            return ResponseUtils.createNotFoundResponse(String.valueOf(msg), HttpStatus.NOT_FOUND.value());

        } catch (Exception e) {
            log.error("ERROR → Delete Duplicate File | duplicateId={} reason={}", duplicateId, e.getMessage(), e);
            msg.setMsg("Failed to delete duplicate file");

            auditLogUtil.logDocumentAction(
                    empObj,
                    "DuplicateDocument",
                    "Delete",
                    "Failure",
                    null,
                    null,
                    Map.of("error", e.getMessage(), "duplicateId", duplicateId),
                    request
            );

            return ResponseUtils.createFailureResponse(
                    msg,
                    new TypeReference<>() {},
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value()
            );
        }
    }

    @Override
    @Transactional
    public ApiResponse<MessageResponse> deleteAllDuplicatesForOriginal(Integer originalId, HttpServletRequest request) {
        log.info("API CALL → Delete All Duplicates For Original | originalId={}", originalId);

        MessageResponse msg = new MessageResponse();
        Employee empObj = currentUser.getCurrentEmployeeOrThrow();

        try {
            // Find original document
            DocumentDetails originalDoc = documentDetailsRepository.findById(originalId)
                    .orElseThrow(() -> {
                        log.error("Original document not found | originalId={}", originalId);
                        return new ResourceNotFoundException("Original document not found with id: " + originalId);
                    });

            // Find all duplicates for this original
            List<DocumentDetails> duplicates = documentDetailsRepository
                    .findByDocumentIdAndIsDuplicateTrue(originalDoc);

            log.debug("Found {} duplicates for original document {}", duplicates.size(), originalId);

            if (duplicates.isEmpty()) {
                msg.setMsg("No duplicate files found for the original document");
                log.info("No duplicates found for original document {}", originalId);
                return ResponseUtils.createSuccessResponse(msg, new TypeReference<>() {});
            }

            int deletedCount = 0;
            int failedCount = 0;

            // Delete each duplicate
            for (DocumentDetails duplicate : duplicates) {
                try {
                    // Delete physical file
                    String filePath = duplicate.getPath();
                    Path physicalFilePath = Paths.get(documentStoragePath, filePath);

                    if (Files.exists(physicalFilePath)) {
                        try {
                            Files.delete(physicalFilePath);
                            log.debug("Deleted physical file: {}", physicalFilePath);
                        } catch (IOException e) {
                            log.error("Failed to delete physical file: {}", physicalFilePath, e);
                            // Continue with DB deletion even if physical file deletion fails
                        }
                    }

                    // Delete from database
                    documentDetailsRepository.delete(duplicate);
                    deletedCount++;

                    log.debug("Successfully deleted duplicate with id: {}", duplicate.getId());

                } catch (Exception e) {
                    log.error("Failed to delete duplicate with id: {}", duplicate.getId(), e);
                    failedCount++;
                }
            }

            // Update original document to remove duplicate flag
            if (deletedCount > 0) {
                // You can update original document properties here
                // For example: originalDoc.setHasDuplicates(false);
            }

            msg.setMsg(String.format("Deleted %d duplicate files. Failed: %d", deletedCount, failedCount));

            // Audit log
            Map<String, Object> detailsJson = new HashMap<>();
            detailsJson.put("originalId", originalId);
            detailsJson.put("originalFileName", originalDoc.getDocName());
            detailsJson.put("deletedCount", deletedCount);
            detailsJson.put("failedCount", failedCount);
            detailsJson.put("totalDuplicates", duplicates.size());

            auditLogUtil.logDocumentAction(
                    empObj,
                    "DuplicateDocument",
                    "DeleteAll",
                    deletedCount > 0 ? "Success" : "Failure",
                    originalId,
                    null,
                    detailsJson,
                    request
            );

            log.info("SUCCESS → Deleted {} duplicates for original document {}", deletedCount, originalId);

            return ResponseUtils.createSuccessResponse(msg, new TypeReference<>() {});

        } catch (ResourceNotFoundException e) {
            log.error("FAILED → Delete All Duplicates | originalId={} reason=Original Not Found", originalId);
            msg.setMsg("Original document not found");

            auditLogUtil.logDocumentAction(
                    empObj,
                    "DuplicateDocument",
                    "DeleteAll",
                    "Failure",
                    originalId,
                    null,
                    Map.of("error", e.getMessage()),
                    request
            );

            return ResponseUtils.createNotFoundResponse(String.valueOf(msg), HttpStatus.NOT_FOUND.value());

        } catch (Exception e) {
            log.error("ERROR → Delete All Duplicates | originalId={} reason={}", originalId, e.getMessage(), e);
            msg.setMsg("Failed to delete duplicate files");

            auditLogUtil.logDocumentAction(
                    empObj,
                    "DuplicateDocument",
                    "DeleteAll",
                    "Failure",
                    originalId,
                    null,
                    Map.of("error", e.getMessage()),
                    request
            );

            return ResponseUtils.createFailureResponse(
                    msg,
                    new TypeReference<>() {},
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value()
            );
        }
    }

    // Helper method to find duplicates for a document
    public List<DocumentDetails> findDuplicatesForDocument(DocumentDetails originalDoc) {
        if (originalDoc == null) {
            return Collections.emptyList();
        }
        return documentDetailsRepository.findByDocumentIdAndIsDuplicateTrue(originalDoc);
    }
}