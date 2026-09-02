package com.dmsBackend.response;

import lombok.Data;

@Data
public class DashboardResponse {
    private long totalUser;
    private long branchUser;
    private long totalDocument;
    private long pendingDocument;
    private long storageUsed;
    private long totalBranches;
    private long totalFilesType;
    private long totalDepartment;
    private long totalRoles;
    private long documentType;
    private long annualYear;
    private long totalCategories;
    private long totalLanguage;

    private long totalApprovedDocuments;
    private long totalRejectedDocuments;
    private long totalPendingDocuments;
    private long totalApprovedDocumentsById;
    private long totalRejectedDocumentsById;
    private long totalPendingDocumentsById;
    private long totalDocumentsById;

    private long totalNullEmployeeType;

    private long totalApprovedStatusDocById;
    private long totalRejectedStatusDocById;

    private long departmentCountForBranch;
    private long nullRoleEmployeeCountForBranch;

    private long departmentUser;
    private long nullRoleEmployeeCountForDepartment;
    private long totalDocumentsByDepartmentId;
    private long totalPendingDocumentsByDepartmentId;
    private long totalApprovedStatusDocByDepartmentId;
    private long totalRejectedStatusDocByDepartmentId;

    private long createdByCount;
    private long pendingDocsbyid;
    private long approvedDocsbyid;
    private long rejectedDocsbyid;

    private long totalUserApplications;
    private long totalTemplate;

    private long trashTotalDoc;
    private long trashTotalDocByEmpId;
    private long trashTotalDocByBranch;
    private long trashTotalDocByDepartment;

    // ───────── Dispatch counts ─────────
    private long dispatchedToday;
    private long dispatchPending;
    private long dispatchedTodayByBranch;
    private long dispatchPendingByBranch;
    private long dispatchedTodayByDepartment;
    private long dispatchPendingByDepartment;



}
