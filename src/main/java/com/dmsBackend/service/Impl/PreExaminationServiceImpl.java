package com.dmsBackend.service.Impl;

import com.dmsBackend.entity.*;
import com.dmsBackend.exception.ResourceNotFoundException;
import com.dmsBackend.repository.*;
import com.dmsBackend.response.*;
import com.dmsBackend.service.PreExaminationService;
import com.dmsBackend.utils.CurrentUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PreExaminationServiceImpl implements PreExaminationService {

    @Autowired private DocumentHeaderRepository documentHeaderRepository;
    @Autowired private PreExaminationRepository preExaminationRepository;
    @Autowired private DocumentDetailsRepository documentDetailsRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private CrimeTypeMasterRepository crimeTypeMasterRepository;
    @Autowired private PriorityMasterRepository priorityMasterRepository;
    @Autowired private PurposeMasterRepository purposeMasterRepository;
    @Autowired private NatureOfExaminationMasterRepository natureOfExaminationMasterRepository;
    @Autowired private SealStatusMasterRepository sealStatusMasterRepository;
    @Autowired private ParcelConditionMasterRepository parcelConditionMasterRepository;
    @Autowired private CurrentUser currentUser;
    @Autowired private DepartmentMasterRepository departmentMasterRepository;
    @Autowired private DocumentForwardingAuthorityRepository documentForwardingAuthorityRepository;
    @Autowired private RoleMasterRepository roleMasterRepository;
    @Autowired private EmployeeRoleRepository employeeRoleRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PreExaminationCaseResponse> getPendingPreExamCases() {
        log.info("API CALL → Get Pending Pre-Examination Cases");

        List<DocumentHeader> approvedHeaders =
                documentHeaderRepository.findAllByApprovalStatusesOrdered(List.of(DocApprovalStatus.APPROVED.name()));

        List<PreExaminationCaseResponse> result = new ArrayList<>();

        for (DocumentHeader header : approvedHeaders) {
            Optional<PreExamination> existing = preExaminationRepository.findByDocumentHeader_Id(header.getId());
            if (existing.isPresent() && existing.get().getStatus() == PreExamination.PreExamStatus.COMPLETED) {
                continue;
            }
            result.add(toCaseResponse(header, existing.orElse(null)));
        }

        log.info("SUCCESS → Found {} cases pending pre-examination", result.size());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public PreExaminationCaseResponse getCaseForPreExam(Integer documentHeaderId) {
        DocumentHeader header = documentHeaderRepository.findById(documentHeaderId)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found: " + documentHeaderId));
        PreExamination existing = preExaminationRepository.findByDocumentHeader_Id(documentHeaderId).orElse(null);
        return toCaseResponse(header, existing);
    }

    @Transactional
    @Override
    public ApiResponse<MessageResponse> savePreExamination(PreExaminationRequest req) {
        log.info("API CALL → Save Pre-Examination | documentHeaderId={}", req.getDocumentHeaderId());

        MessageResponse msg = new MessageResponse();
        ApiResponse<MessageResponse> api = new ApiResponse<>();

        Employee actor = currentUser.getCurrentEmployeeOrThrow();

        try {
            DocumentHeader header = documentHeaderRepository
                    .findById(req.getDocumentHeaderId())
                    .orElseThrow(() -> new ResourceNotFoundException("Case not found: " + req.getDocumentHeaderId()));

            PreExamination preExam = preExaminationRepository
                    .findByDocumentHeader_Id(header.getId())
                    .orElseGet(() -> {
                        PreExamination fresh = new PreExamination();
                        fresh.setDocumentHeader(header);
                        fresh.setCreatedOn(new Timestamp(System.currentTimeMillis()));
                        return fresh;
                    });

            preExam.setPurpose(resolveOrNull(purposeMasterRepository, req.getPurposeId(), "Purpose"));
            preExam.setNatureOfExamination(resolveOrNull(natureOfExaminationMasterRepository, req.getNatureOfExaminationId(), "Nature of Examination"));
            preExam.setNoOfParcels(req.getNoOfParcels());
            preExam.setNoOfExhibits(req.getNoOfExhibits());
            preExam.setNatureOfCase(req.getNatureOfCase());
            preExam.setCrimeType(resolveOrNull(crimeTypeMasterRepository, req.getCrimeTypeId(), "Crime Type"));
            preExam.setPriority(resolveOrNull(priorityMasterRepository, req.getPriorityId(), "Priority"));
            preExam.setSealStatus(resolveOrNull(sealStatusMasterRepository, req.getSealStatusId(), "Seal Status"));
            preExam.setSealVerificationRemarks(req.getSealVerificationRemarks());
            preExam.setParcelCondition(resolveOrNull(parcelConditionMasterRepository, req.getParcelConditionId(), "Parcel Condition"));
            preExam.setParcelConditionOther(req.getParcelConditionOther());
            preExam.setStatus(PreExamination.PreExamStatus.COMPLETED);
            preExam.setExaminedBy(actor.getEmail());
            preExam.setExaminedOn(new Timestamp(System.currentTimeMillis()));
            preExam.setUpdatedOn(new Timestamp(System.currentTimeMillis()));

            preExaminationRepository.save(preExam);

            // Evidence Assignments
            if (req.getAssignments() != null && !req.getAssignments().isEmpty()) {
                for (PreExaminationRequest.EvidenceAssignment a : req.getAssignments()) {
                    if (a.getDocumentDetailId() == null) {
                        throw new IllegalArgumentException("Document detail ID is required");
                    }

                    DocumentDetails detail = documentDetailsRepository
                            .findById(a.getDocumentDetailId())
                            .orElseThrow(() -> new ResourceNotFoundException("Evidence file not found: " + a.getDocumentDetailId()));

                    DepartmentMaster division = null;
                    if (a.getDivisionId() != null) {
                        division = departmentMasterRepository
                                .findById(a.getDivisionId())
                                .orElseThrow(() -> new ResourceNotFoundException("Division not found: " + a.getDivisionId()));
                    }

                    Employee assignedEmployee = null;
                    if (a.getEmployeeId() != null) {
                        assignedEmployee = employeeRepository
                                .findById(a.getEmployeeId())
                                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + a.getEmployeeId()));

                        if (division != null && assignedEmployee.getDepartment() != null &&
                                !assignedEmployee.getDepartment().getId().equals(division.getId())) {
                            throw new IllegalArgumentException("Employee does not belong to the selected division");
                        }
                    }

                    detail.setDepartment(division);
                    detail.setEmpId(assignedEmployee);
                    detail.setAssignmentRemark(a.getRemark());

                    documentDetailsRepository.save(detail);

                    log.info("Evidence assigned | detailId={} | divisionId={} | employeeId={}",
                            detail.getId(), a.getDivisionId(), a.getEmployeeId());
                }
            }

            msg.setMsg("Pre-examination saved successfully");
            api.setStatus(200);
            api.setMessage("Success");
            api.setResponse(msg);

            log.info("SUCCESS → Pre-examination saved | documentHeaderId={}", header.getId());

        } catch (Exception e) {
            log.error("FAILED → Save Pre-Examination | reason={}", e.getMessage(), e);
            msg.setMsg("Failed to save pre-examination: " + e.getMessage());
            api.setStatus(500);
            api.setMessage(msg.getMsg());
            api.setResponse(msg);
        }

        return api;
    }

    @Override
    public List<EmployeeResponse> getScientificOfficersByDivision(Integer divisionId) {
        log.info("API CALL → Get Scientific Officers for Division: {}", divisionId);

        try {
            // Dynamically find employees with SCIENTIFIC OFFICER role from EmpRole table
            List<Employee> employees = employeeRoleRepository
                    .findActiveEmployeesByRoleNameAndDepartmentId("SCIENTIFIC OFFICER", divisionId);

            log.info("Found {} scientific officers for division {}", employees.size(), divisionId);

            return employees.stream()
                    .map(this::toEmployeeResponse)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error fetching scientific officers for division {}: {}", divisionId, e.getMessage());

            // Fallback: Try to get all active employees in the division
            try {
                List<Employee> fallbackEmployees = employeeRepository.findByDepartmentIdAndIsActiveTrue(divisionId);
                log.info("Fallback: Found {} employees for division {}", fallbackEmployees.size(), divisionId);
                return fallbackEmployees.stream()
                        .map(this::toEmployeeResponse)
                        .collect(Collectors.toList());
            } catch (Exception fallbackError) {
                log.error("Fallback also failed for division {}: {}", divisionId, fallbackError.getMessage());
                return List.of();
            }
        }
    }

    private EmployeeResponse toEmployeeResponse(Employee employee) {
        EmployeeResponse response = new EmployeeResponse();
        response.setId(employee.getId());
        response.setName(employee.getName());
        response.setEmail(employee.getEmail());
        response.setMobile(employee.getMobile());

        if (employee.getDepartment() != null) {
            response.setDepartmentName(employee.getDepartment().getName());
        }

        if (employee.getRole() != null) {
            response.setRoleName(employee.getRole().getRole());
        }

        return response;
    }

    private <M extends BaseMasterEntity> M resolveOrNull(JpaRepository<M, Integer> repository, Integer id, String label) {
        if (id == null) return null;
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(label + " not found: " + id));
    }

    private PreExaminationCaseResponse toCaseResponse(DocumentHeader header, PreExamination preExam) {
        PreExaminationCaseResponse resp = new PreExaminationCaseResponse();

        // Case Details
        resp.setDocumentHeaderId(header.getId());
        resp.setFileNo(header.getFileNo());
        resp.setTitle(header.getTitle());
        resp.setFirNumber(header.getFirNumber());
        resp.setPoliceStation(header.getPoliceStation());
        resp.setDateOfIncident(header.getDateOfIncident());
        resp.setIncidentLocation(header.getIncidentLocation());
        resp.setCaseStatus(header.getApprovalStatus() != null ? header.getApprovalStatus().name() : null);

        // Priority Name
        if (header.getPriority() != null) {
            priorityMasterRepository.findById(header.getPriority().getId())
                    .ifPresent(priority -> resp.setPriorityName(priority.getName()));
        }

        // Forwarding Authority
        documentForwardingAuthorityRepository.findByDocumentHeader_Id(header.getId())
                .ifPresent(authority -> {
                    PreExaminationCaseResponse.ForwardingAuthority fa = new PreExaminationCaseResponse.ForwardingAuthority();
                    fa.setAuthorityName(authority.getAuthorityName());
                    fa.setDesignation(authority.getDesignation());
                    fa.setOrganisation(authority.getOrganisation());
                    fa.setForwardingLetterNumber(authority.getForwardingLetterNumber());
                    resp.setForwardingAuthority(fa);
                });

        // Evidence List
        List<PreExaminationCaseResponse.EvidenceRow> evidenceRows = new ArrayList<>();

        if (header.getDocumentDetails() != null) {
            for (DocumentDetails d : header.getDocumentDetails()) {
                if (Boolean.TRUE.equals(d.getIsDeleted())) {
                    continue;
                }

                PreExaminationCaseResponse.EvidenceRow row = new PreExaminationCaseResponse.EvidenceRow();
                row.setDocumentDetailId(d.getId());
                row.setDocName(d.getDocName());

                row.setEvidenceCategory(header.getCategoryMaster() != null ? header.getCategoryMaster().getName() : null);

                // Evidence Type Name
                if (d.getEvidenceTypeId() != null) {
                    try {
                        row.setEvidenceTypeName(d.getEvidenceTypeId().getName());
                    } catch (Exception e) {
                        log.warn("Could not get evidence type name for detail {}", d.getId());
                    }
                }

                // Assigned Division
                if (d.getDepartment() != null) {
                    row.setAssignedDivisionId(d.getDepartment().getId());
                }

                // Assigned Employee
                if (d.getEmpId() != null) {
                    row.setAssignedEmployeeId(d.getEmpId().getId());
                }

                row.setAssignmentRemark(d.getAssignmentRemark());

                evidenceRows.add(row);
            }
        }

        resp.setEvidenceList(evidenceRows);

        // Pre-Examination Data
        resp.setPreExamStatus(preExam == null ? "NOT_STARTED" : preExam.getStatus().name());

        if (preExam != null) {
            if (preExam.getPurpose() != null) {
                resp.setPurposeId(preExam.getPurpose().getId());
            }

            if (preExam.getNatureOfExamination() != null) {
                resp.setNatureOfExaminationId(preExam.getNatureOfExamination().getId());
            }

            resp.setNoOfParcels(preExam.getNoOfParcels());
            resp.setNoOfExhibits(preExam.getNoOfExhibits());
            resp.setNatureOfCase(preExam.getNatureOfCase());

            if (preExam.getCrimeType() != null) {
                resp.setCrimeTypeId(preExam.getCrimeType().getId());
            }

            if (preExam.getPriority() != null) {
                resp.setExamPriorityId(preExam.getPriority().getId());
            }

            if (preExam.getSealStatus() != null) {
                resp.setSealStatusId(preExam.getSealStatus().getId());
            }

            resp.setSealVerificationRemarks(preExam.getSealVerificationRemarks());

            if (preExam.getParcelCondition() != null) {
                resp.setParcelConditionId(preExam.getParcelCondition().getId());
            }

            resp.setParcelConditionOther(preExam.getParcelConditionOther());
        }

        return resp;
    }
}