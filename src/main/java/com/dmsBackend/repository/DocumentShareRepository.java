package com.dmsBackend.repository;

import com.dmsBackend.entity.DocumentShare;
import com.dmsBackend.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentShareRepository extends JpaRepository<DocumentShare, Long> {

    // Find active shares for a specific document
    @Query("SELECT ds FROM DocumentShare ds WHERE ds.documentDetails.id = :documentId " +
            "AND ds.isActive = true " +
            "AND (ds.endTime IS NULL OR ds.endTime > :currentTime)")
    List<DocumentShare> findActiveSharesForDocument(@Param("documentId") Integer documentId,
                                                    @Param("currentTime") LocalDateTime currentTime);

    // Find active shares for a document header
    @Query("SELECT ds FROM DocumentShare ds WHERE ds.documentHeader.id = :headerId " +
            "AND ds.isActive = true " +
            "AND (ds.endTime IS NULL OR ds.endTime > :currentTime)")
    List<DocumentShare> findActiveSharesForDocumentHeader(@Param("headerId") Integer headerId,
                                                          @Param("currentTime") LocalDateTime currentTime);

    // Find all active shares for a recipient
    @Query("SELECT ds FROM DocumentShare ds WHERE ds.sharedTo.id = :recipientId " +
            "AND ds.isActive = true " +
            "AND (ds.endTime IS NULL OR ds.endTime > :currentTime)")
    List<DocumentShare> findActiveSharesForRecipient(@Param("recipientId") Integer recipientId,
                                                     @Param("currentTime") LocalDateTime currentTime);

    // Find all active shares by a sender within same department
    @Query("SELECT ds FROM DocumentShare ds WHERE ds.sharedBy.id = :senderId " +
            "AND ds.sharedTo.department.id = :departmentId " +
            "AND ds.isActive = true " +
            "AND (ds.endTime IS NULL OR ds.endTime > :currentTime)")
    List<DocumentShare> findActiveSharesBySenderInDepartment(@Param("senderId") Integer senderId,
                                                             @Param("departmentId") Integer departmentId,
                                                             @Param("currentTime") LocalDateTime currentTime);

    // Check if document is already shared to employee
    @Query("SELECT ds FROM DocumentShare ds WHERE ds.documentDetails.id = :documentId " +
            "AND ds.sharedTo.id = :recipientId " +
            "AND ds.isActive = true " +
            "AND (ds.endTime IS NULL OR ds.endTime > :currentTime)")
    Optional<DocumentShare> findExistingActiveShare(@Param("documentId") Integer documentId,
                                                    @Param("recipientId") Integer recipientId,
                                                    @Param("currentTime") LocalDateTime currentTime);

    // Find all shares for document header with recipient info
    @Query("SELECT ds FROM DocumentShare ds WHERE ds.documentHeader.id = :headerId " +
            "AND ds.isActive = true " +
            "ORDER BY ds.createdAt DESC")
    List<DocumentShare> findSharesForDocumentHeader(@Param("headerId") Integer headerId);

    @Query("SELECT ds FROM DocumentShare ds " +
            "LEFT JOIN FETCH ds.documentHeader dh " +
            "LEFT JOIN FETCH dh.documentDetails " +
            "LEFT JOIN FETCH dh.departmentMaster " +
            "LEFT JOIN FETCH dh.categoryMaster " +
            "LEFT JOIN FETCH ds.documentDetails dd " +
            "LEFT JOIN FETCH dd.yearMaster " +
            "LEFT JOIN FETCH ds.sharedBy sb " +
            "LEFT JOIN FETCH ds.sharedTo st " +
            "WHERE sb.id = :senderId " +
            "AND sb.department.id = :departmentId " +
            "AND ds.isActive = true " +
            "AND (ds.endTime IS NULL OR ds.endTime > :now)")
    List<DocumentShare> findActiveSharesBySenderInDepartmentWithDetails(
            @Param("senderId") Integer senderId,
            @Param("departmentId") Integer departmentId,
            @Param("now") LocalDateTime now);

    // === ADD THESE MISSING METHODS ===

    // Find shares by document header ID and active status
    @Query("SELECT ds FROM DocumentShare ds WHERE ds.documentHeader.id = :documentHeaderId AND ds.isActive = true")
    List<DocumentShare> findByDocumentHeaderIdAndIsActiveTrue(@Param("documentHeaderId") Integer documentHeaderId);

    // Find shares by shared by ID and active status
    @Query("SELECT ds FROM DocumentShare ds WHERE ds.sharedBy.id = :sharedById AND ds.isActive = true")
    List<DocumentShare> findBySharedByIdAndIsActiveTrue(@Param("sharedById") Integer sharedById);

    // Count shares by document header ID and active status
    @Query("SELECT COUNT(ds) FROM DocumentShare ds WHERE ds.documentHeader.id = :documentHeaderId AND ds.isActive = true")
    int countByDocumentHeaderIdAndIsActiveTrue(@Param("documentHeaderId") Integer documentHeaderId);

    // Count shares by document header ID
    @Query("SELECT COUNT(ds) FROM DocumentShare ds WHERE ds.documentHeader.id = :documentHeaderId")
    long countByDocumentHeaderId(@Param("documentHeaderId") Integer documentHeaderId);
}