package com.dmsBackend.P5Archive;

import com.dmsBackend.ArchiveWithLTO9.LtoRetentionJob;
import com.dmsBackend.ArchiveWithLTO9.LtoRetentionJobRepository;
import com.dmsBackend.entity.ActionTypeForReport;
import com.dmsBackend.entity.DocumentDetails;
import com.dmsBackend.entity.RetentionPolicy;
import com.dmsBackend.repository.DocumentDetailsRepository;
import com.dmsBackend.repository.RetentionPolicyRepository;
import com.dmsBackend.service.DocumentActivityReportService;
import com.dmsBackend.utils.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.*;
import java.time.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class P5ArchiveStatusScheduler {

    private final RetentionPolicyRepository policyRepo;

    private final P5RequestResponceRepository p5RequestRepo;
    private final P5ApiTransactionsRepository txRepo;
    private final LtoRetentionJobRepository jobRepo;
    private final DocumentDetailsRepository detailRepo;
    private final P5OverviewApiService overviewApi;

    private final P5RestoreApiService statusService;

    @Autowired
    private CurrentUser currentUser;

    @Autowired
    private DocumentActivityReportService documentActivityReportService;

    @Value("${p5.client}")
    private String client;

    @Value("${document.storage.path}")
    private String documentStoragePath;

    @Value("${app.lto.restored.retention-days}")
    private long retentionDays;
    /* ====================== SCHEDULER ====================== */

//    @Scheduled(fixedDelay = 30_000) // per 30 second
//    @Scheduled(fixedRate = 7_200_000)    // per 2 hours
//    @Transactional
//    public void resolveArchiveStatus() {
//
//        List<RetentionPolicy> policies =
//                policyRepo.findByArchiveStatus("IN_PROGRESS");
//
//        log.info("⏳ Scheduler started | policies IN_PROGRESS={}", policies.size());
//
//        for (RetentionPolicy policy : policies) {
//
//            log.info("📌 Processing policyId={}", policy.getId());
//
//            P5ArchiveOverviewResponse overview =
//                    overviewApi.fetchOverview(policy);
//
//            if (overview == null || overview.getArchiveOverview() == null) {
//                log.warn("⚠️ No overview for policyId={}", policy.getId());
//                continue;
//            }
//
//            log.info("📦 Overview entries count={}",
//                    overview.getArchiveOverview().size());
//
//            List<P5ApiTransactions> txs =
//                    txRepo.findByRetentionPolicyIdAndApiType(
//                            policy.getId(), "ADDFILES"
//                    );
//
//            log.info("🔁 Found {} ADDFILES tx for policyId={}",
//                    txs.size(), policy.getId());
//
//            for (P5ApiTransactions tx : txs) {
//
//                log.info("➡️ Matching txId={} createdAt={} expectedSizeKb={}",
//                        tx.getId(), tx.getCreatedAt(), tx.getExpectedSizeKb());
//
//                Optional<P5ArchiveOverviewResponse.ArchiveItem> match =
//                        matchExecution(tx, overview.getArchiveOverview());
//
//                if (match.isEmpty()) {
//                    log.warn("❌ No matching overview item for txId={}", tx.getId());
//                    continue;
//                }
//
//                var item = match.get();
//                log.info("✅ Match found | status={} size={} startTime={}",
//                        item.getStatus(), item.getSizeKbytes(), item.getStartTime());
//
//                switch (item.getStatus()) {
//                    case "finished" -> handleSuccess(policy);
//                    case "error", "cancelled" -> handleFailure(policy, item.getStatus());
//                }
//            }
//        }
//    }

    /* ====================== MATCHER ====================== */

    private Optional<P5ArchiveOverviewResponse.ArchiveItem> matchExecution(
            P5ApiTransactions tx,
            List<P5ArchiveOverviewResponse.ArchiveItem> items
    ) {

        Instant txInstant =
                tx.getCreatedAt()
                        .atZone(ZoneId.systemDefault())   // IST
                        .withZoneSameInstant(ZoneOffset.UTC)
                        .toInstant();

        log.info("🕒 TX createdAt(LocalDateTime)={} → txInstant(UTC)={}",
                tx.getCreatedAt(), txInstant);

        return items.stream()
                .filter(i -> client.equalsIgnoreCase(i.getClient()))
                .filter(i -> {

                    Instant overviewInstant = i.getStartTime();

                    long diffSeconds =
                            Math.abs(Duration.between(
                                    txInstant,
                                    overviewInstant
                            ).toSeconds());

                    log.info(
                            "⏱ Compare txInstant={} overviewInstant={} diff={}s",
                            txInstant,
                            overviewInstant,
                            diffSeconds
                    );

                    return diffSeconds <= 30; // ⬅️ keep 30s safety
                })
                .filter(i -> sizeMatches(
                        tx.getExpectedSizeKb(),
                        i.getSizeKbytes()
                ))
                .findFirst();
    }

    private boolean sizeMatches(Long expectedKb, String actualKbStr) {

        if (expectedKb == null || actualKbStr == null)
            return true;

        long actualKb = Long.parseLong(actualKbStr);
        long diff = Math.abs(actualKb - expectedKb);

        long tolerance = Math.max(10, expectedKb * 5 / 100);
        return diff <= tolerance;
    }

    /* ====================== SUCCESS ====================== */

    private void handleSuccess(RetentionPolicy policy) {

        log.info("🟢 Handling SUCCESS for policyId={}", policy.getId());

        LtoRetentionJob job =
                jobRepo.findByRetentionPolicyId(policy.getId())
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "LTO job NOT found for policy " + policy.getId()
                                )
                        );

        log.info("🔗 LTO Job found | jobId={} status={}",
                job.getId(), job.getStatus());

        job.setStatus(LtoRetentionJob.JobStatus.COMPLETED);
        job.setCompletedOn(LocalDateTime.now());
        job.setArchivedFiles(job.getTotalFiles());
        job.setArchivedHeaders(job.getTotalHeaders());
        jobRepo.save(job);

        Long ltoJobId = job.getId();
        log.info("🔍 Fetching DocumentDetails by ltoJobId='{}'", ltoJobId); //45

        P5RequestResponce p5RR = p5RequestRepo.findByLtoRetentionJobId(ltoJobId);

        List<DocumentDetails> docs =
                detailRepo.findByLtoJobId(p5RR.getP5JobId());

        log.info("📄 Documents found count={}", docs.size());

        if (docs.isEmpty()) {
            log.error("❌ NO DOCUMENTS FOUND for ltoJobId='{}'", p5RR.getP5JobId());
        }

        for (DocumentDetails d : docs) {
            log.info("📝 Updating documentId={} oldStatus={}",
                    d.getId(), d.getLtoStatus());

            d.setLtoArchived(true);
            d.setLtoArchivedOn(LocalDateTime.now());
            d.setLtoStatus(LtoRetentionJob.JobStatus.COMPLETED.name());
            d.setLtoFailureReason(null);

            String hlpLog = "lto:";
            String archiveLocation = hlpLog + "/" + d.getPath();
            String jobId = d.getLtoJobId();
            documentActivityReportService.logAction(
                    d.getDocumentHeader(),
                    d,
                    ActionTypeForReport.ARCHIVE,
                    "SUCCESS",
                    currentUser.getCurrentEmployeeOrThrow(),
                    null,
                    Map.of(
                            "jobId", jobId,
                            "location", archiveLocation
                    )
            );
        }

        String result = clearStorageWhenP5success(docs);

        if ("SUCCESS".equals(result)) {
            log.info("✅ All Archived document delete operations completed");
        }


        detailRepo.saveAll(docs);
        log.info("✅ Documents updated successfully count={}", docs.size());

        policy.setArchiveStatus("COMPLETED");
        policy.setIsActive(false);
        policyRepo.save(policy);

        log.info("🏁 Policy COMPLETED policyId={}", policy.getId());
    }

    /* ====================== FAILURE ====================== */

    private void handleFailure(RetentionPolicy policy, String reason) {

        log.error("🔴 Handling FAILURE for policyId={} reason={}",
                policy.getId(), reason);

        LtoRetentionJob job =
                jobRepo.findByRetentionPolicyId(policy.getId())
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "LTO job NOT found for policy " + policy.getId()
                                )
                        );

        job.setStatus(LtoRetentionJob.JobStatus.FAILED);
        job.setFailureReason("P5 status: " + reason);
        job.setFailedFiles(job.getTotalFiles());
        job.setFailedHeaders(job.getTotalHeaders());
        jobRepo.save(job);

        Long ltoJobId = job.getId();
        log.info("🔍 Fetching DocumentDetails by ltoJobId='{}'", ltoJobId);

        P5RequestResponce p5RR = p5RequestRepo.findByLtoRetentionJobId(ltoJobId);

        List<DocumentDetails> docs =
                detailRepo.findByLtoJobId(p5RR.getP5JobId());

        log.info("📄 Documents found count={}", docs.size());

        for (DocumentDetails d : docs) {
            d.setLtoStatus(LtoRetentionJob.JobStatus.FAILED.name());
            d.setLtoFailureReason("P5 status: " + reason);
            d.setLtoArchived(false);

            String hlpLog = "lto:";
            String archiveLocation = hlpLog + "/" + d.getPath();
            String jobId = d.getLtoJobId();
            documentActivityReportService.logAction(
                    d.getDocumentHeader(),
                    d,
                    ActionTypeForReport.ARCHIVE,
                    "FAILED",
                    currentUser.getCurrentEmployeeOrThrow(),
                    null,
                    Map.of(
                            "jobId", jobId,
                            "location", archiveLocation
                    )
            );
        }

        detailRepo.saveAll(docs);

        policy.setArchiveStatus("FAILED");
        policyRepo.save(policy);
    }

    /* ====================== CLEAR STORAGE  ====================== */

    public String clearStorageWhenP5success(List<DocumentDetails> documentDetails) {

        for (DocumentDetails document : documentDetails) {

            if (document.getPath() == null || document.getPath().trim().isEmpty()) {
                log.warn("⚠️ Skipping delete | documentId={} path is null/empty",
                        document.getId());
                continue;
            }

            Path filePath = Paths.get(documentStoragePath, document.getPath()).normalize();

            try {
                log.info("🗑️ Attempting delete | documentId={} path={}",
                        document.getId(), filePath.toAbsolutePath());

                if (Files.notExists(filePath)) {
                    log.error("❌ File not found | {}", filePath.toAbsolutePath());
                    continue;
                }

                if (!Files.isWritable(filePath)) {
                    log.error("❌ No write permission | {}", filePath.toAbsolutePath());
                    continue;
                }

                Files.delete(filePath);
                log.info("✅ File deleted | {}", filePath.toAbsolutePath());

            } catch (NoSuchFileException e) {
                log.error("❌ File missing during delete | {}", e.getFile(), e);

            } catch (AccessDeniedException e) {
                log.error("❌ Access denied during delete | {}", e.getFile(), e);

            } catch (Exception e) {
                log.error("❌ Unexpected delete error | documentId={}",
                        document.getId(), e);
            }
        }

        return "SUCCESS";
    }


//    for rtore status

//    @Scheduled(fixedDelay = 30000) // every 30 sec
//    public void pollActiveRestores() {
//
//        List<DocumentDetails> inProgress =
//                detailRepo.findByRestoredStatus(
//                        LtoRetentionJob.JobStatus.IN_PROGRESS.name()
//                );
//
//
//        if (inProgress.isEmpty()) {
//            return; // 🔥 nothing to do
//        }
//
//        log.info("Checking {} active restore jobs", inProgress.size());
//
//        for (DocumentDetails doc : inProgress) {
//            try {
//                if (doc.getApprovedOn() == null) {
//                    log.warn("ApprovedOn is NULL for docId={}", doc.getId());
//                    continue;
//                }
//
//                LocalDateTime approvedOn = doc.getApprovedOn().toLocalDateTime();
//                log.info("Checking retention policy for docId={}, approvedOn={}",
//                        doc.getId(), approvedOn);
//
//                RetentionPolicy polo =
//                        policyRepo.findActivePolicyByApprovedOn(approvedOn);
//
//                if (polo == null) {
//                    log.warn(
//                            "No active retention policy found for docId={}, approvedOn={}",
//                            doc.getId(), approvedOn
//                    );
//                    continue;
//                }
//
//                log.info("Retention policy found for docId={}, archiveName={}",
//                        doc.getId(), polo.getArchiveName());
//
//                statusService.resolveFinalRestoreStatus(
//                        doc.getId(),
//                        doc.getRestoreJobId(),
//                        polo
//                );
//
//            } catch (Exception ex) {
//                log.error("Restore status check failed for docId={}", doc.getId(), ex);
//            }
//        }
//
//    }
//

    // restore clean

    @Scheduled(cron = "${scheduled.clean.time}")
    public void scheduleCleanup() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
        statusService.cleanRestoredFilesOlderThan(cutoffDate);
    }
}
