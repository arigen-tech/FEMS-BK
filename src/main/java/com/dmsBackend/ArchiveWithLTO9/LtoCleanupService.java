package com.dmsBackend.ArchiveWithLTO9;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;

@Service
@Slf4j
public class LtoCleanupService {

    @Value("${document.storage.base.path}")
    private String baseStoragePath;

    private static final String LTO_ROOT = "DMS_Document_Server_Lto";

    public void cleanupJob(LtoRetentionJob job) {

        try {
            String policyFolderName = buildPolicyFolder(job);

            Path ltoPath = Paths.get(baseStoragePath)
                    .resolve(LTO_ROOT)
                    .resolve(policyFolderName)
                    .normalize();

            if (!Files.exists(ltoPath)) {
                log.warn("⚠ LTO path not found: {}", ltoPath);
                return;
            }

            deleteRecursively(ltoPath);

            job.setStatus(LtoRetentionJob.JobStatus.CLEANED);
            log.info("🧹 LTO cleaned: {}", ltoPath);

        } catch (Exception ex) {
            log.error("❌ LTO cleanup failed for Job ID={}", job.getId(), ex);
        }
    }

    // 🔒 Recursive delete (SAFE)
    private void deleteRecursively(Path path) throws IOException {
        Files.walk(path)
                .sorted((a, b) -> b.compareTo(a)) // delete children first
                .forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    // 🏷 Same naming logic you already use
    private String buildPolicyFolder(LtoRetentionJob job) {

        return String.format(
                "ARCH_%s-%s_AllBr_AllDept_AllCat",
                job.getRetentionPolicy().getFromDate(),
                job.getRetentionPolicy().getToDate()
        );
    }
}
