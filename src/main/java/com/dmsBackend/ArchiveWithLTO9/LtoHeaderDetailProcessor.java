package com.dmsBackend.ArchiveWithLTO9;

import com.dmsBackend.P5Archive.*;
import com.dmsBackend.entity.DocumentDetails;
import com.dmsBackend.entity.DocumentHeader;
import com.dmsBackend.entity.RetentionPolicy;
import com.dmsBackend.repository.DocumentDetailsRepository;
import com.dmsBackend.repository.DocumentHeaderRepository;
import com.dmsBackend.repository.RetentionPolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LtoHeaderDetailProcessor {

    private final DocumentDetailsRepository detailRepo;
    private final DocumentHeaderRepository headerRepo;
    private final LtoRetentionJobRepository jobRepo;
    private final LocalFileTransferService fileService;
    private final RetentionPolicyRepository retentionPolicyRepository;

//    for p5
    private final P5ArchiveApiService p5ArchiveApiService;
    private final P5ApiTransactionsRepository apiTxRepo;
    private final P5RequestResponceRepository requestResponceRepo;



//    For Local TransferF

    @Transactional
    public void processHeader(
            DocumentHeader header,
            List<DocumentDetails> details,
            LtoRetentionJob job
    ) {

        boolean headerFailed = false;

        for (DocumentDetails detail : details) {
            try {
                fileService.archive(detail,job.getRetentionPolicy().getId());
                detailRepo.save(detail);

                job.setArchivedFiles(job.getArchivedFiles() + 1);

            } catch (Exception ex) {
                headerFailed = true;
                job.setFailedFiles(job.getFailedFiles() + 1);
                detailRepo.save(detail);
            }
        }

        if (headerFailed) {
            job.setFailedHeaders(job.getFailedHeaders() + 1);
        } else {
            header.setLtoArchived(true);
            header.setLtoArchivedOn(LocalDateTime.now());
            job.setArchivedHeaders(job.getArchivedHeaders() + 1);
        }

        headerRepo.save(header);
    }


//    For P5 Transfer


//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    public void processHeader(
//            DocumentHeader header,
//            List<DocumentDetails> details,
//            LtoRetentionJob job,
//            RetentionPolicy policy
//    ) {
//
//        P5RequestResponce request = new P5RequestResponce();
//        request.setLtoRetentionJob(job);
//        request.setRetentionPolicy(policy);
//        requestResponceRepo.saveAndFlush(request);
//
//        try {
//            P5AttachResult result =
//                    p5ArchiveApiService.archiveViaApi(details, request, policy);
//
//            for (DocumentDetails d : details) {
//
//                String absPath = Paths.get(
//                        policy.getArchiveName(), d.getPath()
//                ).toString().replace("\\", "/");
//
//                d.setLtoArchived(false);
//                d.setLtoArchivedOn(null);
//                d.setLtoTapePath(result.getPathToHandle().get(absPath));
//                d.setLtoStatus(LtoRetentionJob.JobStatus.IN_PROGRESS.name());
//                d.setLtoJobId(result.getJobId());
//                d.setLtoError(null);
//                d.setLtoFailureReason(null);
//
//                detailRepo.save(d);
//
//                job.setTotalFiles(job.getTotalFiles() + 1);
//            }
//
//            job.setStatus(LtoRetentionJob.JobStatus.IN_PROGRESS);
//            policy.setArchiveStatus(LtoRetentionJob.JobStatus.IN_PROGRESS.name());
//
//        } catch (Exception ex) {
//
//            log.error("❌ P5 attach failed", ex);
//
//            for (DocumentDetails d : details) {
//                d.setLtoArchived(false);
//                d.setLtoStatus(LtoRetentionJob.JobStatus.FAILED.name());
//                d.setLtoError("P5_ATTACH_FAILED");
//                d.setLtoFailureReason(ex.getMessage());
//                detailRepo.save(d);
//
//                job.setFailedFiles(job.getFailedFiles() + 1);
//            }
//
//            job.setStatus(LtoRetentionJob.JobStatus.FAILED);
//            policy.setArchiveStatus(LtoRetentionJob.JobStatus.FAILED.name());
//            job.setFailureReason(ex.getMessage());
//        }
//
//        jobRepo.save(job);
//        retentionPolicyRepository.save(policy);
//    }
}
