//package com.dmsBackend.ArchiveCodes;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
//@Component
//@Slf4j
//public class RetentionScheduler {
//
//    private final ArchiveService archiveService;
//
//    public RetentionScheduler(ArchiveService archiveService) {
//        this.archiveService = archiveService;
//    }
//
//    /**
//     * Run automatic archive every 30 seconds
//     */
//    @Scheduled(fixedRate = 7200000) // 2 hr = 7200000, 30 sec = 30000
//    public void scheduleArchiveJob() {
//        log.info("🔍 Checking RetentionPolicies for archiving at {}", java.time.LocalDateTime.now());
//        archiveService.executeDueArchives();
//    }
//}
