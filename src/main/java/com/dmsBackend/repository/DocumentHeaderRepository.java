package com.dmsBackend.repository;

import com.dmsBackend.entity.*;
import com.dmsBackend.response.DocumentResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@EnableJpaRepositories
public interface DocumentHeaderRepository extends JpaRepository<DocumentHeader, Integer>, JpaSpecificationExecutor<DocumentHeader> {

    // Find document by file number
    Optional<DocumentHeader> findByFileNo(String fileNo);

    // Find all documents by approval status
    @Query(value = """
            SELECT *
            FROM document_header d
            WHERE d.approval_status IN (:approvalStatuses)
            ORDER BY
                CASE
                    WHEN d.is_active = TRUE AND DATE(d.updated_on) = CURRENT_DATE THEN 0
                    WHEN d.is_active = TRUE THEN 1
                    ELSE 2
                END ASC,
                d.updated_on DESC
            """, nativeQuery = true)
    List<DocumentHeader> findAllByApprovalStatusesOrdered(@Param("approvalStatuses") List<String> approvalStatuses);


    @Query(value = """
    SELECT DISTINCT d.*
    FROM document_header d
    JOIN document_details dd
        ON dd.document_header_id = d.id
    WHERE d.if_rejected = true
      AND dd.status = 'REJECTED'
      AND dd.is_deleted = false
    ORDER BY
        CASE
            WHEN d.is_active = TRUE AND DATE(d.updated_on) = CURRENT_DATE THEN 0
            WHEN d.is_active = TRUE THEN 1
            ELSE 2
        END ASC,
        d.updated_on DESC
    """, nativeQuery = true)
    List<DocumentHeader> findAllRejectedForAdmin();


    @Query(value = """
    SELECT DISTINCT d.*
    FROM document_header d
    JOIN document_details dd
        ON dd.document_header_id = d.id
    WHERE d.if_rejected = true
      AND d.branch_id = :branchId
      AND dd.status = 'REJECTED'
      AND dd.is_deleted = false
    ORDER BY
        CASE
            WHEN d.is_active = TRUE AND DATE(d.updated_on) = CURRENT_DATE THEN 0
            WHEN d.is_active = TRUE THEN 1
            ELSE 2
        END ASC,
        d.updated_on DESC
    """, nativeQuery = true)
    List<DocumentHeader> findAllRejectedByBranch(
            @Param("branchId") Integer branchId
    );

    @Query(value = """
    SELECT DISTINCT d.*
    FROM document_header d
    JOIN document_details dd
        ON dd.document_header_id = d.id
    WHERE d.if_rejected = true
      AND d.department_id = :departmentId
      AND dd.status = 'REJECTED'
      AND dd.is_deleted = false
    ORDER BY
        CASE
            WHEN d.is_active = TRUE AND DATE(d.updated_on) = CURRENT_DATE THEN 0
            WHEN d.is_active = TRUE THEN 1
            ELSE 2
        END ASC,
        d.updated_on DESC
    """, nativeQuery = true)
    List<DocumentHeader> findAllRejectedByDepartment(
            @Param("departmentId") Integer departmentId
    );


    List<DocumentHeader> findAllByEmployeeId(Integer employeeId);  // Get all documents for an employee

    // Find all documents by employee ID and approval status
    @Query(value = """
    SELECT DISTINCT d.*
    FROM document_header d
    JOIN document_details dd
        ON dd.document_header_id = d.id
    WHERE d.employee_id = :employeeId
      AND d.if_rejected = true
      AND dd.status = 'REJECTED'
      AND dd.is_deleted = false
    ORDER BY
        CASE
            WHEN d.is_active = TRUE AND DATE(d.updated_on) = CURRENT_DATE THEN 0
            WHEN d.is_active = TRUE THEN 1
            ELSE 2
        END ASC,
        d.updated_on DESC
    """, nativeQuery = true)
    List<DocumentHeader> findAllRejectedByEmployeeId(
            @Param("employeeId") Integer employeeId
    );


    @Query(value = """
    SELECT DISTINCT d.*
    FROM document_header d
    JOIN document_details dd
        ON dd.document_header_id = d.id
    WHERE d.employee_id = :employeeId
      AND d.if_pending = true
      AND dd.status = 'PENDING'
      AND dd.is_deleted = false
    ORDER BY
        CASE
            WHEN d.is_active = TRUE AND DATE(d.updated_on) = CURRENT_DATE THEN 0
            WHEN d.is_active = TRUE THEN 1
            ELSE 2
        END ASC,
        d.updated_on DESC
    """, nativeQuery = true)
    List<DocumentHeader> findAllPendingByEmployeeId(
            @Param("employeeId") Integer employeeId
    );



    @Query(value = """
    SELECT DISTINCT d.*
    FROM document_header d
    JOIN document_details dd
        ON dd.document_header_id = d.id
    WHERE d.employee_id = :employeeId
      AND d.if_approved = true
      AND dd.status = 'APPROVED'
      AND dd.is_deleted = false
    ORDER BY
        CASE
            WHEN d.is_active = TRUE AND DATE(d.updated_on) = CURRENT_DATE THEN 0
            WHEN d.is_active = TRUE THEN 1
            ELSE 2
        END ASC,
        d.updated_on DESC
    """, nativeQuery = true)
    List<DocumentHeader> findAllApprovedByEmployeeId(
            @Param("employeeId") Integer employeeId
    );


    @Query(value = """
    SELECT DISTINCT d.*
    FROM document_header d
    JOIN document_details dd
        ON dd.document_header_id = d.id
    WHERE d.if_pending = true
      AND dd.status = 'PENDING'
      AND dd.is_deleted = false
    ORDER BY
        CASE
            WHEN d.is_active = TRUE AND DATE(d.updated_on) = CURRENT_DATE THEN 0
            WHEN d.is_active = TRUE THEN 1
            ELSE 2
        END ASC,
        d.updated_on DESC
    """, nativeQuery = true)
    List<DocumentHeader> findAllPendingForAdmin();



    @Query(value = """
    SELECT DISTINCT d.*
    FROM document_header d
    JOIN document_details dd
        ON dd.document_header_id = d.id
    WHERE d.if_pending = true
      AND d.branch_id = :branchId
      AND dd.status = 'PENDING'
      AND dd.is_deleted = false
    ORDER BY
        CASE
            WHEN d.is_active = TRUE AND DATE(d.updated_on) = CURRENT_DATE THEN 0
            WHEN d.is_active = TRUE THEN 1
            ELSE 2
        END ASC,
        d.updated_on DESC
    """, nativeQuery = true)
    List<DocumentHeader> findPendingByBranch(
            @Param("branchId") Integer branchId
    );


    @Query(value = """
    SELECT DISTINCT d.*
    FROM document_header d
    JOIN document_details dd
        ON dd.document_header_id = d.id
    WHERE d.if_pending = true
      AND d.department_id = :departmentId
      AND dd.status = 'PENDING'
      AND dd.is_deleted = false
    ORDER BY
        CASE
            WHEN d.is_active = TRUE AND DATE(d.updated_on) = CURRENT_DATE THEN 0
            WHEN d.is_active = TRUE THEN 1
            ELSE 2
        END ASC,
        d.updated_on DESC
    """, nativeQuery = true)
    List<DocumentHeader> findPendingByDepartment(
            @Param("departmentId") Integer departmentId
    );



    // Find documents by employee ID, approval status, and a date range
    List<DocumentHeader> findAllByEmployeeIdAndApprovalStatusAndUpdatedOnBetween(
            Integer employeeId, DocApprovalStatus approvalStatus, Timestamp startDate, Timestamp endDate
    );


    // Count documents by approval status
    long countByApprovalStatusIn(List<DocApprovalStatus> approvalStatus);

    // Count documents by employee ID and approval status
    long countByApprovalStatusAndEmployeeId(DocApprovalStatus approvalStatus, Integer employeeId);

    // Count total documents for a specific employee
    long countByEmployeeId(Integer employeeId);


    // Custom queries to count approved/rejected documents by employee who approved/rejected
    @Query("SELECT COUNT(d) FROM DocumentHeader d WHERE d.employee.id = :employeeId AND d.approvalStatus = :approvalStatus")
    long countByEmployeeByAndApprovalStatus(
            @Param("employeeId") Integer employeeId,
            @Param("approvalStatus") DocApprovalStatus approvalStatus);

    // Find all approved/rejected documents handled by a specific employee
    @Query("SELECT d FROM DocumentHeader d WHERE d.employee.id = :employeeId AND d.approvalStatus = :approvalStatus")
    List<DocumentHeader> findAllByEmployeeByAndApprovalStatus(
            @Param("employeeId") Integer employeeId,
            @Param("approvalStatus") DocApprovalStatus approvalStatus);

    // graph cout

    @Query("SELECT MONTH(d.createdOn), COUNT(d) FROM DocumentHeader d WHERE d.employee.id = :employeeId AND d.approvalStatus = :approvalStatus GROUP BY MONTH(d.createdOn)")
    List<Object[]> countByEmployeeAndApprovalStatusGroupedByMonth(
            @Param("employeeId") Integer employeeId,
            @Param("approvalStatus") DocApprovalStatus approvalStatus);

    @Query("SELECT dh FROM DocumentHeader dh " +
            "WHERE dh.employee.branch.id = :branchId " +
            "AND dh.approvalStatus IN (:statuses) " +
            "ORDER BY dh.updatedOn DESC, dh.createdOn DESC")
    List<DocumentHeader> findPendingByBranch(@Param("branchId") Integer branchId, @Param("statuses") List<DocApprovalStatus> statuses);

    List<DocumentHeader> findByEmployee_IdAndApprovalStatus(Integer employeeId, DocApprovalStatus approvalStatus);

    @Query("""
                SELECT d FROM DocumentHeader d
                WHERE d.employee.branch = :branch
                  AND d.approvalStatus IN :statuses
                ORDER BY
                    CASE
                        WHEN d.active = true AND FUNCTION('DATE', d.updatedOn) = CURRENT_DATE THEN 0
                        WHEN d.active = true THEN 1
                        ELSE 2
                    END,
                    d.updatedOn DESC
            """)
    List<DocumentHeader> findAllApprovedInBranchOrdered(
            @Param("branch") BranchMaster branch,
            @Param("statuses") List<DocApprovalStatus> statuses);


    @Query("""
                SELECT d FROM DocumentHeader d
                WHERE d.employee.branch = :branch
                  AND d.approvalStatus IN :statuses
                ORDER BY
                    CASE
                        WHEN d.active = true AND FUNCTION('DATE', d.updatedOn) = CURRENT_DATE THEN 0
                        WHEN d.active = true THEN 1
                        ELSE 2
                    END,
                    d.updatedOn DESC
            """)
    List<DocumentHeader> findAllRejectedByBranch(
            @Param("branch") BranchMaster branch,
            @Param("statuses") List<DocApprovalStatus> statuses);


    // Find documents by admin's ID and approval status
    // List<DocumentHeader> findByEmployeeByIdAndApprovalStatus(Integer employeeId, DocApprovalStatus approvalStatus);

    Long countByEmployee_BranchId(Integer branch);

    Long countByEmployee_BranchIdAndApprovalStatusIn(Integer branch, List<DocApprovalStatus> statuses);

    @Query("""
                SELECT d FROM DocumentHeader d
                WHERE d.employee.department = :department
                  AND d.approvalStatus IN :statuses
                ORDER BY
                    CASE
                        WHEN d.active = true AND FUNCTION('DATE', d.updatedOn) = CURRENT_DATE THEN 0
                        WHEN d.active = true THEN 1
                        ELSE 2
                    END,
                    d.updatedOn DESC
            """)
    List<DocumentHeader> findAllApprovedInDepartmentOrdered(
            @Param("department") DepartmentMaster department,
            @Param("statuses") List<DocApprovalStatus> statuses);

    @Query("""
                SELECT d FROM DocumentHeader d
                WHERE d.employee.department = :department
                  AND d.approvalStatus IN :statuses
                ORDER BY
                    CASE
                        WHEN d.active = true AND FUNCTION('DATE', d.updatedOn) = CURRENT_DATE THEN 0
                        WHEN d.active = true THEN 1
                        ELSE 2
                    END,
                    d.updatedOn DESC
            """)
    List<DocumentHeader> findAllRejectedByDepartment(
            @Param("department") DepartmentMaster department,
            @Param("statuses") List<DocApprovalStatus> statuses);


    /**
     * Custom query to search for documents based on various criteria.
     *
     * @param fileNo         The file number to search for.
     * @param title          The title to search for.
     * @param subject        The subject to search for.
     * @param version        The version to search for.
     * @return A list of DocumentHeader entities matching the criteria.
     */
    @Query("""
SELECT DISTINCT d
FROM DocumentHeader d
JOIN d.documentDetails dd
WHERE (:fileNo IS NULL OR LOWER(d.fileNo) LIKE LOWER(CONCAT('%', :fileNo, '%')))
AND (:title IS NULL OR LOWER(d.title) LIKE LOWER(CONCAT('%', :title, '%')))
AND (:subject IS NULL OR LOWER(d.subject) LIKE LOWER(CONCAT('%', :subject, '%')))
AND (:version IS NULL OR LOWER(dd.version) LIKE LOWER(CONCAT('%', :version, '%')))

AND (:categoryId IS NULL OR d.categoryMaster.id = :categoryId)
AND (:branchId IS NULL OR d.branchMaster.id = :branchId)
AND (:departmentId IS NULL OR d.departmentMaster.id = :departmentId)

AND (
    :metadataKey IS NULL OR EXISTS (
        SELECT 1 FROM DocumentMetadata m
        WHERE m.documentHeader = d
        AND LOWER(m.metaKey) = LOWER(:metadataKey)
        AND LOWER(m.metaValue) LIKE LOWER(CONCAT('%', :metadataValue, '%'))
    )
)
""")
    List<DocumentHeader> searchDocuments(
            @Param("fileNo") String fileNo,
            @Param("title") String title,
            @Param("subject") String subject,
            @Param("version") String version,
            @Param("categoryId") Integer categoryId,
            @Param("branchId") Integer branchId,
            @Param("departmentId") Integer departmentId,
            @Param("metadataKey") String metadataKey,
            @Param("metadataValue") String metadataValue
    );



    // Pending documents by Department - ordered with most recently updated first
    @Query("SELECT dh FROM DocumentHeader dh " +
            "WHERE dh.employee.department.id = :departmentId " +
            "AND dh.approvalStatus IN (:statuses) " +
            "ORDER BY dh.updatedOn DESC, dh.createdOn DESC")
    List<DocumentHeader> findPendingByDepartment(
            @Param("departmentId") Integer departmentId,
            @Param("statuses") List<DocApprovalStatus> statuses);


    //for download report
    @Query("""
                SELECT new com.dmsBackend.response.DocumentResponse(
                    dh.title, dh.fileNo, dh.subject,
                    cm.name, b.name, d.name,
                    dh.createdOn, dh.approvalStatus, COUNT(dd.id))
                FROM DocumentHeader dh
                JOIN dh.documentDetails dd
                JOIN dh.employee e
                JOIN dh.categoryMaster cm
                JOIN e.branch b
                JOIN e.department d
                WHERE (:categoryId IS NULL OR dh.categoryMaster.id = :categoryId)
                  AND (:approvalStatus IS NULL OR dh.approvalStatus = :approvalStatus)
                  AND dh.createdOn BETWEEN :startDate AND :endDate
                  AND e.branch.id = :branchId
                  AND e.department.id = :departmentId
                GROUP BY dh.title, dh.fileNo, dh.subject, 
                         cm.name, b.name, d.name, 
                         dh.createdOn, dh.approvalStatus
            """)
    List<DocumentResponse> findFilteredDocuments(
            @Param("categoryId") Integer categoryId,
            @Param("approvalStatus") DocApprovalStatus approvalStatus,
            @Param("startDate") Timestamp startDate,
            @Param("endDate") Timestamp endDate,
            @Param("branchId") Integer branchId,
            @Param("departmentId") Integer departmentId
    );


    @Query("""
            SELECT new com.dmsBackend.response.DocumentResponse(
                dh.title, dh.fileNo, dh.subject,
                cm.name, b.name, d.name,
                dh.createdOn, dh.approvalStatus, COUNT(DISTINCT dd.id))
            FROM DocumentHeader dh
            JOIN dh.documentDetails dd
            JOIN dh.employee e
            JOIN dh.categoryMaster cm
            JOIN e.branch b
            JOIN e.department d
            WHERE (:categoryId IS NULL OR cm.id = :categoryId)
              AND (:approvalStatus IS NULL OR dh.approvalStatus = :approvalStatus)
              AND (:startDate IS NULL OR dh.createdOn >= :startDate)
              AND (:endDate IS NULL OR dh.createdOn <= :endDate)
              AND (:branchId IS NULL OR b.id = :branchId)
              AND (:departmentId IS NULL OR d.id = :departmentId)
              AND (:employeeId IS NULL OR e.id = :employeeId)
            GROUP BY dh.title, dh.fileNo, dh.subject, 
                     cm.name, b.name, d.name, 
                     dh.createdOn, dh.approvalStatus
            """)
    List<DocumentResponse> findFilteredDocumentsById(
            @Param("categoryId") Integer categoryId,
            @Param("approvalStatus") DocApprovalStatus approvalStatus,
            @Param("startDate") Timestamp startDate,
            @Param("endDate") Timestamp endDate,
            @Param("branchId") Integer branchId,
            @Param("departmentId") Integer departmentId,
            @Param("employeeId") Integer employeeId
    );


    Optional<DocumentHeader> findByQrPath(String qrPath);

    long countByEmployee_Department_Id(Integer departmentId);

    long countByEmployee_Department_IdAndApprovalStatusIn(Integer departmentId, List<DocApprovalStatus> docApprovalStatuses);

    //    @Query("SELECT COUNT(d) FROM DocumentHeader d WHERE d.employee.id = :employeeId AND d.approvalStatus = :approvalStatus")
//    long countByEmployeeIdAndApprovalStatus(@Param("employeeId") Integer employeeId, @Param("approvalStatus") DocApprovalStatus approvalStatus);
    long countByEmployeeIdAndApprovalStatusIn(Integer employeeId, List<DocApprovalStatus> approvalStatuses);


    @Query("""
                SELECT MONTH(d.createdOn), d.approvalStatus, COUNT(d)
                FROM DocumentHeader d
                WHERE d.employee.id = :employeeId
                  AND d.createdOn BETWEEN :startDate AND :endDate
                GROUP BY MONTH(d.createdOn), d.approvalStatus
                ORDER BY MONTH(d.createdOn)
            """)
    List<Object[]> countByEmployeeGroupedByStatusAndMonth(
            @Param("employeeId") Integer employeeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );


    @Query("""
                SELECT MONTH(dh.createdOn), dh.approvalStatus, COUNT(dh)
                FROM DocumentHeader dh
                JOIN dh.employee e
                WHERE e.branch.id = :branchId
                  AND dh.createdOn BETWEEN :startDate AND :endDate
                GROUP BY MONTH(dh.createdOn), dh.approvalStatus
                ORDER BY MONTH(dh.createdOn)
            """)
    List<Object[]> countByBranch(
            @Param("branchId") Integer branchId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );


    @Query("""
                SELECT MONTH(dh.createdOn), dh.approvalStatus, COUNT(dh)
                FROM DocumentHeader dh
                JOIN dh.employee e
                WHERE e.department.id = :departmentId
                  AND dh.createdOn BETWEEN :startDate AND :endDate
                GROUP BY MONTH(dh.createdOn), dh.approvalStatus
                ORDER BY MONTH(dh.createdOn)
            """)
    List<Object[]> countByDepartment(
            @Param("departmentId") Integer departmentId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("""
                SELECT MONTH(dh.createdOn) AS month,
                       dh.approvalStatus AS status,
                       COUNT(dh.id) AS totalCount
                FROM DocumentHeader dh
                WHERE dh.createdOn BETWEEN :startDate AND :endDate
                GROUP BY MONTH(dh.createdOn), dh.approvalStatus
                ORDER BY MONTH(dh.createdOn)
            """)
    List<Object[]> countTotalByMonth(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );


    //    top 10 brach
    @Query("""
                SELECT dh.employee.branch.name AS branchName,
                       dh.approvalStatus AS status,
                       COUNT(dh.id) AS totalCount
                FROM DocumentHeader dh
                WHERE dh.createdOn BETWEEN :startDate AND :endDate
                GROUP BY dh.employee.branch.name, dh.approvalStatus
            """)
    List<Object[]> countStatusByBranch(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );


    @Query("""
                SELECT dh.employee.branch.name AS branchName,
                       COUNT(dh.id) AS total
                FROM DocumentHeader dh
                WHERE dh.createdOn BETWEEN :startDate AND :endDate
                GROUP BY dh.employee.branch.name
                ORDER BY total DESC
            """)
    List<Object[]> findTopBranchesByCount(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );


    @Query("SELECT d FROM DocumentHeader d WHERE d.employee.department.id = :departmentId AND d.createdOn BETWEEN :startDate AND :endDate")
    List<DocumentHeader> findByDepartmentAndDateRange(
            @Param("departmentId") Integer departmentId,
            @Param("startDate") Timestamp startDate,
            @Param("endDate") Timestamp endDate
    );

    @Query("SELECT d FROM DocumentHeader d WHERE d.employee.branch.id = :branchId AND d.createdOn BETWEEN :startDate AND :endDate")
    List<DocumentHeader> findByBranchAndDateRange(
            @Param("branchId") Integer branchId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // Method to find documents by branch (through the employee's branch)
    @Query("SELECT d FROM DocumentHeader d WHERE d.employee.branch.id = :branchId")
    List<DocumentHeader> findByBranch(@Param("branchId") Integer branchId);

    // Method to find documents by department (through the employee's department)
    @Query("SELECT d FROM DocumentHeader d WHERE d.employee.department.id = :departmentId")
    List<DocumentHeader> findByDepartment(@Param("departmentId") Integer departmentId);

    @Query("SELECT d FROM DocumentHeader d WHERE d.categoryMaster.id = :categoryId AND d.employee.department.id = :departmentId")
    List<DocumentHeader> findByDepartmentAndCategory(@Param("departmentId") Integer departmentId, @Param("categoryId") Integer categoryId);

    List<DocumentHeader> findByCategoryMaster_Id(Integer categoryId);

    List<DocumentHeader> findByEmployeeDepartmentIdAndCategoryMasterId(Integer departmentId, Integer categoryId);

    List<DocumentHeader> findByEmployeeDepartmentId(Integer departmentId);

    @Query("SELECT dh FROM DocumentHeader dh " +
            "WHERE dh.employee.department.id = :departmentId " +
            "AND dh.approvalStatus = 'APPROVED'")
    List<DocumentHeader> findByDepartmentAndApproved(@Param("departmentId") Integer departmentId);

//    @Query("SELECT dh FROM DocumentHeader dh " +
//            "WHERE dh.employee.department.id = :departmentId " +
//            "AND dh.approvalStatus = 'APPROVED' " +
//            "AND dh.approvalStatusOn BETWEEN :fromDate AND :toDate")
//    List<DocumentHeader> findByDepartmentAndDateRangeAndApproved(
//            @Param("departmentId") Integer departmentId,
//            @Param("fromDate") Timestamp fromDate,
//            @Param("toDate") Timestamp toDate);


    @Query("""
                SELECT DISTINCT h
                FROM DocumentHeader h
                JOIN h.documentDetails d
                WHERE d.status = com.dmsBackend.entity.DocApprovalStatus.APPROVED
                  AND d.archive = false
                  AND d.approvedOn BETWEEN :fromDate AND :toDate
                  AND (:branchId IS NULL OR h.employee.branch.id = :branchId)
                  AND (:deptId IS NULL OR h.employee.department.id = :deptId)
                  AND (:categoryId IS NULL OR h.categoryMaster.id = :categoryId)
            """)
    List<DocumentHeader> findApprovedDocsWithinPeriod(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("branchId") Integer branchId,
            @Param("deptId") Integer deptId,
            @Param("categoryId") Integer categoryId
    );


    @Query(value = """
            SELECT *
            FROM document_header d
            ORDER BY
                CASE
                    WHEN d.is_active = TRUE AND DATE(d.updated_on) = CURRENT_DATE THEN 0
                    WHEN d.is_active = TRUE THEN 1
                    ELSE 2
                END ASC,
                d.updated_on DESC
            """, nativeQuery = true)
    List<DocumentHeader> findAllDocumentHeadersOrdered();


    @Query("SELECT d FROM DocumentHeader d " +
            "JOIN d.employee e " +
            "WHERE (:branchId IS NULL OR e.branch.id = :branchId) " +
            "AND (:departmentId IS NULL OR e.department.id = :departmentId) " +
            "AND (:employeeId IS NULL OR e.id = :employeeId) " +
            "ORDER BY d.createdOn DESC")
    List<DocumentHeader> findFilteredDocuments(@Param("branchId") Integer branchId,
                                               @Param("departmentId") Integer departmentId,
                                               @Param("employeeId") Integer employeeId);


    @Query("""
                select distinct dh
                from DocumentHeader dh
                join dh.employee e
                where dh.active = true
                  and dh.ltoArchived = false
                  and dh.approvalStatus = 'APPROVED'
                  and (:fromDate is null or dh.createdOn >= :fromDate)
                  and (:toDate   is null or dh.createdOn <= :toDate)
                  and (:branch   is null or e.branch = :branch)
                  and (:department is null or e.department = :department)
                  and (:category is null or dh.categoryMaster = :category)
            """)
    List<DocumentHeader> findEligibleForRetention(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("branch") BranchMaster branch,
            @Param("department") DepartmentMaster department,
            @Param("category") CategoryMaster category
    );

/* =====================================================
   ADMIN
   ===================================================== */

    // ✅ Approved + Partially Approved (NOT Deleted)
    @Query("""
    SELECT DISTINCT dh
    FROM DocumentHeader dh
    JOIN FETCH dh.documentDetails dd
    WHERE dh.ifApproved = true
      AND dd.isDeleted = false
      AND dd.status = 'APPROVED'
    ORDER BY dh.updatedOn DESC
""")
    List<DocumentHeader> findApprovedHeadersForAdmin();


    @Query("""
    SELECT DISTINCT dh
    FROM DocumentHeader dh
    JOIN FETCH dh.documentDetails dd
    WHERE dd.isDeleted = true
    ORDER BY dh.updatedOn DESC
""")
    List<DocumentHeader> findDeletedDetailsForAdmin();


/* =====================================================
   BRANCH ADMIN
   ===================================================== */

    // ✅ Approved + Partially Approved (NOT Deleted)
    @Query("""
    SELECT DISTINCT dh
    FROM DocumentHeader dh
    JOIN FETCH dh.documentDetails dd
    WHERE dh.ifApproved = true
      AND dh.employee.branch = :branch
      AND dd.isDeleted = false
      AND dd.status = 'APPROVED'
    ORDER BY dh.updatedOn DESC
""")
    List<DocumentHeader> findApprovedHeadersByBranch(
            @Param("branch") BranchMaster branch
    );

    @Query("""
    SELECT DISTINCT dh
    FROM DocumentHeader dh
    JOIN FETCH dh.documentDetails dd
    WHERE dh.branchMaster = :branch
      AND dd.isDeleted = true
    ORDER BY dh.updatedOn DESC
""")
    List<DocumentHeader> findDeletedDetailsByBranch(
            @Param("branch") BranchMaster branch
    );


/* =====================================================
   DEPARTMENT ADMIN
   ===================================================== */

    // ✅ Approved + Partially Approved (NOT Deleted)
    @Query("""
    SELECT DISTINCT dh
    FROM DocumentHeader dh
    JOIN FETCH dh.documentDetails dd
    WHERE dh.ifApproved = true
      AND dh.employee.department = :department
      AND dd.isDeleted = false
      AND dd.status = 'APPROVED'
    ORDER BY dh.updatedOn DESC
""")
    List<DocumentHeader> findApprovedHeadersByDepartment(
            @Param("department") DepartmentMaster department
    );


    @Query("""
    SELECT DISTINCT dh
    FROM DocumentHeader dh
    JOIN FETCH dh.documentDetails dd
    WHERE dh.departmentMaster = :department
      AND dd.isDeleted = true
    ORDER BY dh.updatedOn DESC
""")
    List<DocumentHeader> findDeletedDetailsByDepartment(
            @Param("department") DepartmentMaster department
    );




//    lto

    List<DocumentHeader> findByLtoArchivedFalse();

    @Query("SELECT h FROM DocumentHeader h WHERE h.createdOn < :cutoffDate AND h.ltoArchived = false")
    List<DocumentHeader> findEligibleForArchiving(@Param("cutoffDate") LocalDateTime cutoffDate);

    List<DocumentHeader> findByLtoStatus(String status);


    @Query("""
        select distinct h from DocumentHeader h
        join fetch h.documentDetails d
        where h.approvalStatus = 'APPROVED'
          and h.active = true
          and h.ltoArchived = false
          and d.ltoArchived = false
    """)
    List<DocumentHeader> findEligibleForLto();

    List<DocumentHeader> findByApprovalStatusAndEmployeeAndBranchMasterAndDepartmentMaster(DocApprovalStatus approved, Employee employee, BranchMaster branch, DepartmentMaster department);

    boolean existsByFileNo(String fileNo);




    @Query("""
        SELECT d FROM DocumentHeader d 
        WHERE d.id IN :documentIds
          AND d.branchMaster = :branch
          AND d.departmentMaster = :department
    """)
    List<DocumentHeader> findAllByIdAndBranchAndDepartment(
            @Param("documentIds") List<Integer> documentIds,
            @Param("branch") BranchMaster branch,
            @Param("department") DepartmentMaster department
    );


    @Query("""
    SELECT DISTINCT h
    FROM DocumentHeader h
    JOIN FETCH h.documentDetails d
    WHERE FUNCTION('DATE', d.approvedOn) BETWEEN :from AND :to
      AND h.branchMaster = :branch
      AND h.departmentMaster = :department
      AND h.externalApiFlag = true
""")
    List<DocumentHeader> findHeadersByDetailApprovedDateBetweenAndBranchAndDepartment(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("branch") BranchMaster branch,
            @Param("department") DepartmentMaster department
    );



}

