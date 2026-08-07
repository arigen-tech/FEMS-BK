package com.dmsBackend.ArchiveWithLTO9;

import com.dmsBackend.entity.RetentionPolicy;
import com.dmsBackend.response.CartridgeInfo;
import com.dmsBackend.utils.DetectCurrCartridge;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LtoRetentionExecutionService {

    private final LtoRetentionJobRepository jobRepository;
    private final LtoRetentionProcessor processor;

    private final LtoCleanupService cleanupService;

    @Value("${current.ltfs.drive}")
    private String currentLtfsDrive;



//    @Scheduled(cron = "${scheduled.archive.time}")
//@Scheduled(cron = "0 0 0 * * *")
@Scheduled(cron = "*/30 * * * * *")
public void executeRetentionJobs() {

        log.info("🔄 Retention scheduler triggered");

        CartridgeInfo cartridgeInfo = DetectCurrCartridge.detect(currentLtfsDrive);

        if (cartridgeInfo == null || cartridgeInfo.getCartridge() == null) {
            log.warn("⚠️ No LTO cartridge detected. Scheduler will retry later.");
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        jobRepository.findPendingJobs()
                .forEach(job -> {

                    RetentionPolicy policy = job.getRetentionPolicy();
                    LocalDateTime retentionDateTime = policy.getRetentionDateTime();

                    if (retentionDateTime == null) {
                        log.warn("⚠️ Job {} has no retention datetime, skipping", job.getId());
                        return;
                    }
                    if (now.isBefore(retentionDateTime)) {
                        log.info(
                                "⏳ Job {} scheduled for {}, current time {} — skipping",
                                job.getId(),
                                retentionDateTime,
                                now
                        );
                        return;
                    }

                    log.info("📦 Dispatching Job ID={}", job.getId());
                    processor.processAsync(job.getId());
                });
    }


//    @Scheduled(cron = "0 0 2 * * *")
//    public void cleanupExpiredLto() {
//
//        LocalDate cutoffDate = LocalDate.now().minusDays(2);
//
//        log.info("🧹 LTO cleanup scheduler triggered. Cutoff={}", cutoffDate);
//
//        List<LtoRetentionJob> jobs =
//                jobRepository.findEligibleForCleanup(cutoffDate);
//
//        for (LtoRetentionJob job : jobs) {
//            cleanupService.cleanupJob(job);
//            jobRepository.save(job);
//        }
//    }
}

