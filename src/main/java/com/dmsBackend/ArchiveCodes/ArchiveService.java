package com.dmsBackend.ArchiveCodes;

import com.dmsBackend.entity.DocumentDetails;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface ArchiveService {

    @Transactional
    void executeDueArchives();

    @jakarta.transaction.Transactional
    void retryFailedArchives(Long archiveJobId);

    List<ArchiveJobDTO> getArchiveJobs(Integer branch, Integer department, ArchiveJob.Status status);

    ArchivedDelete deleteObject(ArchivedDeleteRequest request);

    List<DocumentHeaderArchivedDTO> getGroupedArchives(Long archiveJobId);

    List<DocumentDetails> getArchivedDocs(Integer documentHeaderId, Long archiveJobId, String version);
}
