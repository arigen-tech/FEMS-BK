package com.dmsBackend.repository;


import com.dmsBackend.ArchiveCodes.ArchiveJob;
import com.dmsBackend.entity.DocumentArchive;
import com.dmsBackend.entity.DocumentHeader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DocumentArchiveRepository extends JpaRepository<DocumentArchive, Integer> {
    Optional<DocumentArchive> findFirstByArchiveJobAndDocumentHeaderAndObjectName(ArchiveJob archiveJob,
                                                                                  DocumentHeader documentHeader,
                                                                                  String objectName);

    List<DocumentArchive> findByArchiveJob(ArchiveJob job);
    List<DocumentArchive> findByArchiveJobId(Long jobId);

    @Query("""
        SELECT da 
        FROM DocumentArchive da
        JOIN FETCH da.documentHeader dh
        JOIN FETCH dh.employee e
        LEFT JOIN FETCH e.branch b
        LEFT JOIN FETCH e.department d
        WHERE da.archiveJob.id = :archiveJobId
    """)
    List<DocumentArchive> findByArchivedJobId(@Param("archiveJobId") Long archiveJobId);

}
