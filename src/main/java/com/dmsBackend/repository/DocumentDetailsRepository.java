package com.dmsBackend.repository;

import com.dmsBackend.ArchiveCodes.ArchiveJob;
import com.dmsBackend.entity.DocApprovalStatus;
import com.dmsBackend.entity.DocumentArchive;
import com.dmsBackend.entity.DocumentDetails;
import com.dmsBackend.entity.DocumentHeader;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentDetailsRepository extends JpaRepository<DocumentDetails, Integer> {


    long countByDocumentHeaderId(Integer headerId);
    long countByDocumentHeaderIdAndStatus(Integer headerId, DocApprovalStatus status);

    List<DocumentDetails> findByDocumentHeader(DocumentHeader documentHeader);
    List<DocumentDetails> findByDocumentHeaderId(Integer documentHeaderId);

    @Query("""
    SELECT d FROM DocumentDetails d
    WHERE d.documentHeader.id = :headerId
      AND (:status IS NULL OR d.status = :status)
""")
    List<DocumentDetails> findByHeaderIdAndStatus(
            @Param("headerId") Integer headerId,
            @Param("status") DocApprovalStatus status
    );



    List<DocumentDetails> findByArchiveJobId(Long archiveJobId);

    DocumentDetails findByDocName (String docName);

    void deleteByDocumentHeader(DocumentHeader documentHeader);

    Optional<DocumentDetails> findById(Integer id);

    @Modifying
    @Query("DELETE FROM DocumentDetails d WHERE d.documentHeader = :headerId")
    void deleteByHeaderId(@Param("headerId") Integer headerId);

    Optional<DocumentDetails> findByDocNameAndDocumentHeader(String docName, DocumentHeader documentHeader);

    @Query("SELECT d FROM DocumentDetails d WHERE d.path IS NULL OR NOT d.path LIKE CONCAT(:prefix, '%')")
    List<DocumentDetails> findDocumentsNotArchived(@Param("prefix") String prefix);


    @Query(value = """
    SELECT 
        YEAR(d.created_on) AS year,
        f.file_type AS fileType,
        COUNT(*) AS fileCount
    FROM 
        document_details d
    JOIN 
        file_type_master f 
        ON LOWER(SUBSTRING_INDEX(d.file_name, '.', -1)) = LOWER(SUBSTRING_INDEX(f.extension, '.', -1))
    GROUP BY 
        year, fileType
    ORDER BY 
        year, fileCount DESC
""", nativeQuery = true)
    List<Object[]> findAllFileTypeCountsByYear();


    @Query("SELECT d FROM DocumentDetails d " +
            "WHERE d.status = 'APPROVED' " +
            "AND d.archive = false " +
            "AND (d.ltoStatus <> 'ARCHIVED') " +
            "AND d.approvedOn BETWEEN :fromDate AND :toDate " +
            "AND (:branchId IS NULL OR d.documentHeader.employee.branch.id = :branchId) " +
            "AND (:deptId IS NULL OR d.documentHeader.employee.department.id = :deptId) " +
            "AND (:categoryId IS NULL OR d.documentHeader.categoryMaster.id = :categoryId)")
    List<DocumentDetails> findApprovedDetailsWithinPeriod(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("branchId") Integer branchId,
            @Param("deptId") Integer deptId,
            @Param("categoryId") Integer categoryId
    );


    @Query("SELECT dd FROM DocumentDetails dd WHERE dd.path NOT LIKE :archivePrefix AND dd.documentHeader.approvalStatus = 'APPROVED'")
    List<DocumentDetails> findDocumentsNotArchivedAndApproved(@Param("archivePrefix") String archivePrefix);


    @Query("SELECT d FROM DocumentDetails d WHERE d.path = :filePath")
    DocumentDetails findByFilePath(@Param("filePath") String filePath);

    List<DocumentDetails> findByArchiveJobAndDocumentHeader(ArchiveJob archiveJob, DocumentHeader documentHeader);

    Integer countByArchiveJob_IdAndDocumentHeader_Id(Long archiveJobId, Integer documentHeaderId);


    List<DocumentDetails> findByDocumentArchiveAndArchivalStatus(DocumentArchive archive, String archived);

    @Transactional
    @Modifying
    @Query("UPDATE DocumentDetails d SET d.restored = true WHERE d.documentArchive = :archive")
    int markRestored(@Param("archive") DocumentArchive archive);


    List<DocumentDetails> findByRestoredTrue();

    List<DocumentDetails> findByDocumentArchive(DocumentArchive archive);

    @Query("""
        SELECT dd 
        FROM DocumentDetails dd
        WHERE dd.documentHeader.id = :documentHeaderId
          AND dd.archiveJob.id = :archiveJobId
          AND dd.version = :version
          AND dd.archive = true
    """)
    List<DocumentDetails> findArchivedDocs(
            @Param("documentHeaderId") Integer documentHeaderId,
            @Param("archiveJobId") Long archiveJobId,
            @Param("version") String version);
    List<DocumentDetails> findByIsDuplicateTrue();

    // Find duplicates for a specific original document
    List<DocumentDetails> findByDocumentIdAndIsDuplicateTrue(DocumentDetails originalDoc);

    // Optional: Find by documentId
    List<DocumentDetails> findByDocumentId_Id(Integer originalId);

    long countByIsDeletedTrue();


    @Query("""
        SELECT COUNT(dd)
        FROM DocumentDetails dd
        JOIN dd.documentHeader dh
        WHERE dd.isDeleted = true
        AND dh.employee.id = :empId
    """)
    long countTrashByEmployee(@Param("empId") Integer empId);


    @Query("""
        SELECT COUNT(dd)
        FROM DocumentDetails dd
        JOIN dd.documentHeader dh
        WHERE dd.isDeleted = true
        AND dh.branchMaster.id = :branchId
    """)
    long countTrashByBranch(@Param("branchId") Integer branchId);


    @Query("""
        SELECT COUNT(dd)
        FROM DocumentDetails dd
        JOIN dd.documentHeader dh
        WHERE dd.isDeleted = true
        AND dh.departmentMaster.id = :deptId
    """)
    long countTrashByDepartment(@Param("deptId") Integer deptId);


    List<DocumentDetails> findByLtoJobId(String toString);

    Long countByLtoStatus(String ltoStatus);

    List<DocumentDetails> findByRestoredStatus(String restoredStatus);

    List<DocumentDetails> findByLtoJobIdAndDocumentHeaderId(
            String ltoJobId,
            Integer documentHeaderId
    );


    List<DocumentDetails> findByIdIn(List<Integer> ids);

    @Query("SELECT d FROM DocumentDetails d " +
            "WHERE d.ltoArchived = true " +
            "AND d.ltoRestoredOn IS NOT NULL " +
            "AND d.ltoRestoredOn <= :cutoffDate")
    List<DocumentDetails> findRestoredBefore(@Param("cutoffDate") LocalDateTime cutoffDate);


    // counts new
    @Query("""
    SELECT COUNT(dd)
    FROM DocumentDetails dd
    JOIN dd.documentHeader dh
    WHERE dh.branchMaster.id = :branchId
      AND dd.status = :detailStatus
""")
    Long countApprovedDetailsByBranch(
            @Param("branchId") Integer branchId,
            @Param("detailStatus") DocApprovalStatus detailStatus
    );


    @Query("""
    SELECT COUNT(dd)
    FROM DocumentDetails dd
    JOIN dd.documentHeader dh
    WHERE dh.branchMaster.id = :branchId
""")
    Long countTotalDetailsByBranch(@Param("branchId") Integer branchId);


    @Query("""
    SELECT COUNT(dd)
    FROM DocumentDetails dd
    JOIN dd.documentHeader dh
    WHERE dh.departmentMaster.id  = :departmentId
""")
    Long countTotalDetailsByDepartment(
            @Param("departmentId") Integer departmentId
    );


    @Query("""
    SELECT COUNT(dd)
    FROM DocumentDetails dd
    JOIN dd.documentHeader dh
    WHERE dh.departmentMaster.id = :departmentId
      AND dd.status = :detailStatus
""")
    Long countApprovedDetailsByDepartment(
            @Param("departmentId") Integer departmentId,
            @Param("detailStatus") DocApprovalStatus detailStatus
    );



    @Query("""
    SELECT COUNT(dd)
    FROM DocumentDetails dd
    JOIN dd.documentHeader dh
    JOIN dh.employee e
    WHERE e.id = :employeeId
      AND dd.status = :detailStatus
""")
    Long countApprovedDetailsByEmployee(
            @Param("employeeId") Integer employeeId,
            @Param("detailStatus") DocApprovalStatus detailStatus
    );


    @Query("""
    SELECT COUNT(dd)
    FROM DocumentDetails dd
    WHERE dd.status = :detailStatus
""")
    Long countApprovedDetails(
            @Param("detailStatus") DocApprovalStatus detailStatus
    );


    @Query("""
    SELECT COUNT(dd)
    FROM DocumentDetails dd
""")
    Long countTotalDetails();


    // Find all versions for a document header and year
    @Query("SELECT d FROM DocumentDetails d WHERE d.documentHeader.id = :headerId AND d.yearMaster.id = :yearId ORDER BY d.version DESC")
    List<DocumentDetails> findByDocumentHeaderIdAndYearMasterIdOrderByVersionDesc(
            @Param("headerId") Integer headerId,
            @Param("yearId") Integer yearId
    );

    // Get max version for a document header and year
    @Query("SELECT MAX(d.version) FROM DocumentDetails d WHERE d.documentHeader.id = :headerId AND d.yearMaster.id = :yearId")
    String findMaxVersionByHeaderAndYear(
            @Param("headerId") Integer headerId,
            @Param("yearId") Integer yearId
    );

}

