package com.dmsBackend.ArchiveWithLTO9;

import com.dmsBackend.P5Archive.P5ArchiveApiService;
import com.dmsBackend.P5Archive.P5AttachResult;
import com.dmsBackend.P5Archive.P5RequestResponce;
import com.dmsBackend.P5Archive.P5RequestResponceRepository;
import com.dmsBackend.entity.DocumentDetails;
import com.dmsBackend.entity.DocumentHeader;
import com.dmsBackend.entity.RetentionPolicy;
import com.dmsBackend.repository.DocumentDetailsRepository;
import com.dmsBackend.repository.RetentionPolicyRepository;
import com.dmsBackend.response.CartridgeInfo;
import com.dmsBackend.utils.DetectCurrCartridge;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LtoRetentionProcessor {

    private final LtoRetentionJobRepository jobRepo;
    private final DocumentDetailsRepository detailRepo;
    private final LtoHeaderDetailProcessor headerProcessor;
    private final P5RequestResponceRepository p5RequestResponceRepository;
    private final P5ArchiveApiService p5ArchiveApiService;

    private final RetentionPolicyRepository retentionPolicyRepository;

    @Value("${current.ltfs.drive}")
    private String currentLtfsDrive;




//    for local
@Async
@Transactional
public void processAsync(Long jobId) {
    log.info("🚀 Async job started for JobId={}", jobId);


    LtoRetentionJob job =
            jobRepo.findByIdWithPolicy(jobId).orElseThrow(() -> {
                log.error("Job with JobId={} not found", jobId);
                return new RuntimeException("Job not found");
            });

    log.info("Job found: {} - Status: {}", job.getId(), job.getStatus());

    job.setStatus(LtoRetentionJob.JobStatus.IN_PROGRESS);
    job.setStartedOn(LocalDateTime.now());
    log.info("Job status set to IN_PROGRESS and StartedOn time set");

    try {
        RetentionPolicy policy = job.getRetentionPolicy();
        log.debug("Using retention policy: {}", policy);

        List<DocumentDetails> details =
                detailRepo.findApprovedDetailsWithinPeriod(
                        policy.getFromDate(),
                        policy.getToDate(),
                        policy.getBranch() != null ? policy.getBranch().getId() : null,
                        policy.getDepartment() != null ? policy.getDepartment().getId() : null,
                        policy.getCategory() != null ? policy.getCategory().getId() : null
                );

        if (details.isEmpty()) {
            log.info("No details found for job, completing job.");
            job.setStatus(LtoRetentionJob.JobStatus.COMPLETED);
            job.setCompletedOn(LocalDateTime.now());
            jobRepo.save(job);
            log.info("Job completed successfully with no files to process");
            return;
        }

        log.info("Found {} document details to process", details.size());

        Map<DocumentHeader, List<DocumentDetails>> grouped =
                details.stream()
                        .collect(Collectors.groupingBy(DocumentDetails::getDocumentHeader));

        log.info("Grouped document details by header. Total headers: {}", grouped.size());

        job.setTotalHeaders(grouped.size());
        job.setTotalFiles(details.size());

        grouped.entrySet()
                .parallelStream()
                .forEach(entry -> {
                    log.debug("Processing header: {}", entry.getKey());
                    headerProcessor.processHeader(entry.getKey(), entry.getValue(), job);
                });

        if (job.getFailedFiles() > 0 || job.getFailedHeaders() > 0) {
            log.error("Job failed with {} failed files and {} failed headers", job.getFailedFiles(), job.getFailedHeaders());
            job.setStatus(LtoRetentionJob.JobStatus.FAILED);
            job.setFailureReason("Some files failed during archival");
        } else {
            log.info("Job completed successfully. Deactivating retention policy.");
            job.setStatus(LtoRetentionJob.JobStatus.COMPLETED);
            job.setCompletedOn(LocalDateTime.now());

            // ✅ deactivate policy ONLY if fully successful
            policy.setIsActive(false);
            log.info("Retention policy deactivated");
        }

    } catch (Exception ex) {
        if (ex.getMessage().contains("Tape Full")) {

            job.setStatus(LtoRetentionJob.JobStatus.FAILED);
            job.setFailureReason("Tape Full");

            log.error("❌ Tape full. Insert new cartridge.");
        }
        log.error("Exception occurred during job processing: {}", ex.getMessage(), ex);
        job.setStatus(LtoRetentionJob.JobStatus.FAILED);
        job.setFailureReason(ex.getMessage());

    }

    job.setCompletedOn(LocalDateTime.now());
    jobRepo.save(job);
    log.info("Job with JobId={} completed. Final status: {}", jobId, job.getStatus());
}


//    for p5


//    @Async("retentionExecutor")
//    public void processAsync(Long jobId) {
//
//        log.info("🚀 Async job started for JobId={}", jobId);
//
//        LtoRetentionJob job =
//                jobRepo.findByIdWithPolicy(jobId).orElseThrow();
//
//        job.setStatus(LtoRetentionJob.JobStatus.IN_PROGRESS);
//        job.setStartedOn(LocalDateTime.now());
//        jobRepo.save(job);
//
//        try {
//            RetentionPolicy policy = job.getRetentionPolicy();
//
//            List<DocumentDetails> details =
//                    detailRepo.findApprovedDetailsWithinPeriod(
//                            policy.getFromDate(),
//                            policy.getToDate(),
//                            policy.getBranch() != null ? policy.getBranch().getId() : null,
//                            policy.getDepartment() != null ? policy.getDepartment().getId() : null,
//                            policy.getCategory() != null ? policy.getCategory().getId() : null
//                    );
//
//            if (details.isEmpty()) {
//                job.setCompletedOn(LocalDateTime.now());
//                job.setStatus(LtoRetentionJob.JobStatus.COMPLETED);
//                jobRepo.save(job);
//                return;
//            }
//
//            /* ================= GROUP HEADERS ================= */
//            Map<DocumentHeader, List<DocumentDetails>> grouped =
//                    details.stream()
//                            .collect(Collectors.groupingBy(DocumentDetails::getDocumentHeader));
//
//            job.setTotalHeaders(grouped.size());
//
//            /* ================= FLATTEN ALL FILES ================= */
//            List<DocumentDetails> allDocuments =
//                    grouped.values()
//                            .stream()
//                            .flatMap(List::stream)
//                            .toList();
//
//            job.setTotalFiles(allDocuments.size());
//            jobRepo.save(job);
//
//            /* ================= CREATE ONE REQUEST ================= */
//            P5RequestResponce request = new P5RequestResponce();
//            request.setLtoRetentionJob(job);
//            request.setRetentionPolicy(policy);
//            p5RequestResponceRepository.saveAndFlush(request);
//
//            /* ================= ONE PLAN + ONE ADDFILES ================= */
//            P5AttachResult result =
//                    p5ArchiveApiService.archiveViaApi(
//                            allDocuments,
//                            request,
//                            policy
//                    );
//
//            /* ================= UPDATE ALL DOCUMENTS ================= */
//            for (DocumentDetails d : allDocuments) {
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
//            }
//
//            job.setStatus(LtoRetentionJob.JobStatus.IN_PROGRESS);
//            policy.setArchiveStatus(LtoRetentionJob.JobStatus.IN_PROGRESS.name());
//            retentionPolicyRepository.save(policy);
//        } catch (Exception ex) {
//
//            log.error("❌ Retention job failed", ex);
//
//            job.setStatus(LtoRetentionJob.JobStatus.FAILED);
//            job.setFailureReason(ex.getMessage());
//        }
//
//        job.setCompletedOn(LocalDateTime.now());
//        jobRepo.save(job);
//    }

}
