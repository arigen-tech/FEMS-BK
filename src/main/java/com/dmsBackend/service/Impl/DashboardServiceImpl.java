package com.dmsBackend.service.Impl;

import com.dmsBackend.entity.DocApprovalStatus;
import com.dmsBackend.entity.Employee;
import com.dmsBackend.repository.*;
import com.dmsBackend.response.DashboardResponse;
import com.dmsBackend.service.DashboardService;
import com.dmsBackend.service.DocumentDetailsService;
import com.dmsBackend.service.DocumentHeaderService;
import com.dmsBackend.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    @Autowired
    EmployeeRepository employeeRepository;

    @Autowired
    DocumentHeaderService documentHeaderService;

    @Autowired
    DocumentDetailsService documentDetailsService;

    @Autowired
    EmployeeService employeeService;

    @Autowired
    DocumentDetailsRepository documentDetailRepository;

    @Autowired
    DocumentHeaderRepository documentHeaderRepository;

    @Autowired
    BranchMasterRepository branchMasterRepository;

    @Autowired
    CategoryMasterRepository categoryMasterRepository;

    @Autowired
    DepartmentMasterRepository departmentMasterRepository;

    @Autowired
    RoleMasterRepository roleMasterRepository;

    @Autowired
    TypeMasterRepository typeMasterRepository;

    @Autowired
    YearMasterRepository yearMasterRepository;

    @Autowired
    FilesTypeMasterRepository filesTypeMasterRepository;

    @Autowired
    private UserApplicationRepository userApplicationRepository;

    @Autowired
    private MasTemplateRepo masTemplateRepo;

    @Autowired
    private LanguageMasterRepository languageMasterRepository;

    @Override
    public DashboardResponse getAllUsers(String employeeId) {
        log.info("API CALL → Get Dashboard Data | employeeId={}", employeeId);

        DashboardResponse dashboardResponse = new DashboardResponse();

        // Convert employeeId from String to Integer
        Integer empId = Integer.parseInt(employeeId);

        // Find employee
        Employee employee = employeeRepository.findById(empId).orElseThrow(() -> {
            log.error("FAILED → Get Dashboard Data | employeeId={} reason=Employee Not Found", employeeId);
            return new RuntimeException("Employee not found");
        });

        // Extract branch and department IDs (nullable)
        Integer branchId = employee.getBranch() != null ? employee.getBranch().getId() : null;
        Integer departmentId = employee.getDepartment() != null ? employee.getDepartment().getId() : null;

        log.debug("Employee: {} | Branch ID: {} | Department ID: {}",
                employee.getName(), branchId, departmentId);

        // ───────── General (system-wide) counts ─────────
        // NOTE: totalDocument / pendingDocument below are kept as-is from your
        // original code (detail-row counts) since they're used only as raw
        // storage/file stats, not as the case-approval badges shown in the sidebar.
        dashboardResponse.setTotalUser(employeeRepository.count());
        dashboardResponse.setTotalDocument(documentDetailRepository.countTotalDetails());
        dashboardResponse.setPendingDocument(documentDetailRepository.count());
        dashboardResponse.setStorageUsed(documentDetailRepository.count());
        dashboardResponse.setTotalBranches(branchMasterRepository.count());
        dashboardResponse.setTotalFilesType(filesTypeMasterRepository.count());
        dashboardResponse.setTotalDepartment(departmentMasterRepository.count());
        dashboardResponse.setTotalRoles(roleMasterRepository.count());
        dashboardResponse.setDocumentType(typeMasterRepository.count());
        dashboardResponse.setAnnualYear(yearMasterRepository.count());
        dashboardResponse.setTotalCategories(categoryMasterRepository.count());

        // ───────── HEADER-LEVEL (case-level) approval counts — system-wide ─────────
        dashboardResponse.setTotalApprovedDocuments(documentHeaderService.countApprovedDocuments());
        dashboardResponse.setTotalRejectedDocuments(documentHeaderService.countRejectedDocuments());
        dashboardResponse.setTotalPendingDocuments(documentHeaderService.countPendingDocuments());

        dashboardResponse.setTotalNullEmployeeType(employeeService.countEmployeesByRoleNull());
        dashboardResponse.setTotalLanguage(languageMasterRepository.count());

        dashboardResponse.setTotalUserApplications(userApplicationRepository.count());
        dashboardResponse.setTotalTemplate(masTemplateRepo.count());

        // User-specific counts
        long createdByCount = employeeRepository.countByCreatedById(empId);
        dashboardResponse.setCreatedByCount(createdByCount);

        // ───────── HEADER-LEVEL (case-level) counts for the logged-in employee ─────────
        long pendingCount = documentHeaderService.countPendingDocumentsByEmployeeId(empId);
        long approvedCount = documentHeaderService.countApprovedDocumentsByEmployeeId(empId);
        long rejectedCount = documentHeaderService.countRejectedDocumentsByEmployeeId(empId);

        dashboardResponse.setPendingDocsbyid(pendingCount);
        dashboardResponse.setApprovedDocsbyid(approvedCount);
        dashboardResponse.setRejectedDocsbyid(rejectedCount);

        dashboardResponse.setTrashTotalDoc(
                documentDetailRepository.countByIsDeletedTrue()
        );

        dashboardResponse.setTrashTotalDocByEmpId(
                documentDetailRepository.countTrashByEmployee(empId)
        );

        dashboardResponse.setTrashTotalDocByBranch(
                documentDetailRepository.countTrashByBranch(branchId)
        );

        dashboardResponse.setTrashTotalDocByDepartment(
                documentDetailRepository.countTrashByDepartment(departmentId)
        );

        // Branch-specific counts (if employee has branch)
        if (branchId != null) {
            log.debug("Processing branch-specific counts | branchId={}", branchId);

            // Branch user counts
            dashboardResponse.setBranchUser(employeeRepository.countByBranchId(branchId));
            dashboardResponse.setDepartmentCountForBranch(departmentMasterRepository.countByBranchId(branchId));
            dashboardResponse.setNullRoleEmployeeCountForBranch(employeeRepository.countByBranchIdAndRoleIsNull(branchId));

            // ───────── HEADER-LEVEL (case-level) branch document counts ─────────
            dashboardResponse.setTotalDocumentsById(documentHeaderService.countDocumentHeadersByBranchId(branchId));
            dashboardResponse.setTotalPendingDocumentsById(documentHeaderService.countPendingDocumentsByBranchId(branchId));
            dashboardResponse.setTotalApprovedStatusDocById(documentHeaderService.countApprovedByBranchId(branchId));
            dashboardResponse.setTotalRejectedStatusDocById(documentHeaderService.countRejectedByBranchId(branchId));
        }

        // Department-specific counts (if employee has department)
        if (departmentId != null) {
            log.debug("Processing department-specific counts | departmentId={}", departmentId);

            // Department user counts
            dashboardResponse.setDepartmentUser(employeeRepository.countByDepartmentId(departmentId));
            dashboardResponse.setNullRoleEmployeeCountForDepartment(employeeRepository.countByDepartmentIdAndRoleIsNull(departmentId));

            // ───────── HEADER-LEVEL (case-level) department document counts ─────────
            dashboardResponse.setTotalDocumentsByDepartmentId(documentHeaderService.countDocumentHeadersByDepartmentId(departmentId));
            dashboardResponse.setTotalPendingDocumentsByDepartmentId(documentHeaderService.countPendingDocumentsByDepartmentId(departmentId));
            dashboardResponse.setTotalApprovedStatusDocByDepartmentId(documentHeaderService.countApprovedByDepartmentId(departmentId));
            dashboardResponse.setTotalRejectedStatusDocByDepartmentId(documentHeaderService.countRejectedByDepartmentId(departmentId));
        }

        // For SYSTEM ADMIN (no branch/department)
        if (branchId == null && departmentId == null) {
            log.debug("Processing system admin counts");

            // Admin counts
            dashboardResponse.setBranchUser(employeeRepository.count());
            dashboardResponse.setDepartmentUser(employeeRepository.count());
            dashboardResponse.setNullRoleEmployeeCountForBranch(employeeRepository.countByRoleIsNull());
            dashboardResponse.setNullRoleEmployeeCountForDepartment(employeeRepository.countByRoleIsNull());

            // ───────── HEADER-LEVEL (case-level) admin document counts ─────────
            dashboardResponse.setTotalDocumentsById(documentHeaderRepository.count());
            dashboardResponse.setTotalDocumentsByDepartmentId(documentHeaderRepository.count());
            dashboardResponse.setTotalPendingDocumentsById(documentHeaderService.countPendingDocuments());
            dashboardResponse.setTotalPendingDocumentsByDepartmentId(documentHeaderService.countPendingDocuments());
            dashboardResponse.setTotalApprovedStatusDocById(documentHeaderService.countApprovedDocuments());
            dashboardResponse.setTotalApprovedStatusDocByDepartmentId(documentHeaderService.countApprovedDocuments());
            dashboardResponse.setTotalRejectedStatusDocById(documentHeaderService.countRejectedDocuments());
            dashboardResponse.setTotalRejectedStatusDocByDepartmentId(documentHeaderService.countRejectedDocuments());
        }

        log.info("SUCCESS → Dashboard Data Retrieved | employeeId={} name={} totalUsers={} totalDocs={}",
                employeeId, employee.getName(), dashboardResponse.getTotalUser(), dashboardResponse.getTotalDocument());

        return dashboardResponse;
    }
}