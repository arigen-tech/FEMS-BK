package com.dmsBackend.repository;

import com.dmsBackend.entity.ReportEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReportEntryRepository extends JpaRepository<ReportEntry, Integer> {

    Optional<ReportEntry> findByDocumentHeader_IdAndEmployee_Id(Integer documentHeaderId, Integer employeeId);

    List<ReportEntry> findByStatus(ReportEntry.ReportStatus status);

    @Query("SELECT r FROM ReportEntry r WHERE r.status = 'SUBMITTED' ORDER BY r.submittedOn DESC")
    List<ReportEntry> findAllSubmittedReports();

    // FIX: Handle NULL review_status by treating it as PENDING
    @Query("SELECT r FROM ReportEntry r WHERE r.status = 'SUBMITTED' AND (r.reviewStatus IS NULL OR r.reviewStatus = '' OR r.reviewStatus = 'PENDING') ORDER BY r.submittedOn DESC")
    List<ReportEntry> findPendingReviewReports();

    @Query("SELECT r FROM ReportEntry r WHERE r.reviewStatus = 'APPROVED' ORDER BY r.reviewedOn DESC")
    List<ReportEntry> findApprovedReports();

    @Query("SELECT r FROM ReportEntry r WHERE r.reviewStatus = 'REJECTED' ORDER BY r.reviewedOn DESC")
    List<ReportEntry> findRejectedReports();

    @Query("SELECT r FROM ReportEntry r WHERE r.reviewStatus = 'REFERRED' ORDER BY r.referredOn DESC")
    List<ReportEntry> findReferredReports();

    @Query("SELECT r FROM ReportEntry r WHERE r.reviewStatus = :reviewStatus ORDER BY r.reviewedOn DESC")
    List<ReportEntry> findByReviewStatus(@Param("reviewStatus") String reviewStatus);

    List<ReportEntry> findByDocumentHeader_Id(Integer documentHeaderId);




        // ───────── Dispatch Dashboard Counts ─────────

        // Reports dispatched today — system-wide
        @Query("SELECT COUNT(r) FROM ReportEntry r " +
                "WHERE r.dispatchStatus = 'DISPATCHED' " +
                "AND CAST(r.dispatchedOn AS date) = CURRENT_DATE")
        long countDispatchedToday();

        // Approved reports still awaiting dispatch — system-wide
        @Query("SELECT COUNT(r) FROM ReportEntry r " +
                "WHERE r.reviewStatus = 'APPROVED' " +
                "AND (r.dispatchStatus IS NULL OR r.dispatchStatus <> 'DISPATCHED')")
        long countDispatchPending();

        // ───────── Branch-scoped ─────────
        @Query("SELECT COUNT(r) FROM ReportEntry r " +
                "WHERE r.dispatchStatus = 'DISPATCHED' " +
                "AND CAST(r.dispatchedOn AS date) = CURRENT_DATE " +
                "AND r.employee.branch.id = :branchId")
        long countDispatchedTodayByBranch(@Param("branchId") Integer branchId);

        @Query("SELECT COUNT(r) FROM ReportEntry r " +
                "WHERE r.reviewStatus = 'APPROVED' " +
                "AND (r.dispatchStatus IS NULL OR r.dispatchStatus <> 'DISPATCHED') " +
                "AND r.employee.branch.id = :branchId")
        long countDispatchPendingByBranch(@Param("branchId") Integer branchId);

        // ───────── Department-scoped ─────────
        @Query("SELECT COUNT(r) FROM ReportEntry r " +
                "WHERE r.dispatchStatus = 'DISPATCHED' " +
                "AND CAST(r.dispatchedOn AS date) = CURRENT_DATE " +
                "AND r.employee.department.id = :departmentId")
        long countDispatchedTodayByDepartment(@Param("departmentId") Integer departmentId);

        @Query("SELECT COUNT(r) FROM ReportEntry r " +
                "WHERE r.reviewStatus = 'APPROVED' " +
                "AND (r.dispatchStatus IS NULL OR r.dispatchStatus <> 'DISPATCHED') " +
                "AND r.employee.department.id = :departmentId")
        long countDispatchPendingByDepartment(@Param("departmentId") Integer departmentId);



}