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

        System.out.println("========== DASHBOARD DEBUG START ==========");
        System.out.println("Employee ID: " + empId);
        System.out.println("Employee Name: " + employee.getName());

        log.debug("Employee: {} | Branch ID: {} | Department ID: {}",
                employee.getName(), branchId, departmentId);

        // Set general counts
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
        dashboardResponse.setTotalApprovedDocuments(documentDetailsService.countApprovedDetails(DocApprovalStatus.APPROVED));
        dashboardResponse.setTotalRejectedDocuments(documentDetailsService.countApprovedDetails(DocApprovalStatus.REJECTED));
        dashboardResponse.setTotalPendingDocuments(documentDetailsService.countApprovedDetails(DocApprovalStatus.PENDING));
        dashboardResponse.setTotalNullEmployeeType(employeeService.countEmployeesByRoleNull());
        dashboardResponse.setTotalLanguage(languageMasterRepository.count());

        dashboardResponse.setTotalUserApplications(userApplicationRepository.count());
        dashboardResponse.setTotalTemplate(masTemplateRepo.count());

        // User-specific counts
        long createdByCount = employeeRepository.countByCreatedById(empId);
        dashboardResponse.setCreatedByCount(createdByCount);

        // Approval status lists
        DocApprovalStatus pendingStatuses = DocApprovalStatus.PENDING;
        DocApprovalStatus approvedStatuses = DocApprovalStatus.APPROVED;
        DocApprovalStatus rejectedStatuses = DocApprovalStatus.REJECTED;

        // Employee document counts
        long pendingCount = documentDetailRepository.countApprovedDetailsByEmployee(empId, pendingStatuses);
        long approvedCount = documentDetailRepository.countApprovedDetailsByEmployee(empId, approvedStatuses);
        long rejectedCount = documentDetailRepository.countApprovedDetailsByEmployee(empId, rejectedStatuses);

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
            System.out.println("\n=== BRANCH-SPECIFIC COUNTS ===");

            // Branch user counts
            dashboardResponse.setBranchUser(employeeRepository.countByBranchId(branchId));
            dashboardResponse.setDepartmentCountForBranch(departmentMasterRepository.countByBranchId(branchId));
            dashboardResponse.setNullRoleEmployeeCountForBranch(employeeRepository.countByBranchIdAndRoleIsNull(branchId));

            // Branch document counts
            dashboardResponse.setTotalDocumentsById(documentDetailsService.countTotalDocByBranchId(branchId));
            dashboardResponse.setTotalPendingDocumentsById(documentDetailsService.countPendingDocumentsByBranchId(branchId));
            dashboardResponse.setTotalApprovedStatusDocById(documentDetailsService.countApprovedByBranchId(branchId));
            dashboardResponse.setTotalRejectedStatusDocById(documentDetailsService.countRejectedByBranchId(branchId));
        }

        // Department-specific counts (if employee has department)
        if (departmentId != null) {
            log.debug("Processing department-specific counts | departmentId={}", departmentId);
            System.out.println("\n=== DEPARTMENT-SPECIFIC COUNTS ===");

            // Department user counts
            dashboardResponse.setDepartmentUser(employeeRepository.countByDepartmentId(departmentId));
            dashboardResponse.setNullRoleEmployeeCountForDepartment(employeeRepository.countByDepartmentIdAndRoleIsNull(departmentId));

            // Department document counts
            dashboardResponse.setTotalDocumentsByDepartmentId(documentDetailsService.countTotalDocByDepartmentId(departmentId));
            dashboardResponse.setTotalPendingDocumentsByDepartmentId(documentDetailsService.countPendingDocumentsByDepartmentId(departmentId));
            dashboardResponse.setTotalApprovedStatusDocByDepartmentId(documentDetailsService.countApprovedDetailsByDepartmentId(departmentId));
            dashboardResponse.setTotalRejectedStatusDocByDepartmentId(documentDetailsService.countRejectedByDepartmentId(departmentId));
        }

        // For SYSTEM ADMIN (no branch/department)
        if (branchId == null && departmentId == null) {
            log.debug("Processing system admin counts");
            System.out.println("\n=== SYSTEM ADMIN COUNTS ===");

            // Admin counts
            dashboardResponse.setBranchUser(employeeRepository.count());
            dashboardResponse.setDepartmentUser(employeeRepository.count());
            dashboardResponse.setNullRoleEmployeeCountForBranch(employeeRepository.countByRoleIsNull());
            dashboardResponse.setNullRoleEmployeeCountForDepartment(employeeRepository.countByRoleIsNull());

            // Admin document counts
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