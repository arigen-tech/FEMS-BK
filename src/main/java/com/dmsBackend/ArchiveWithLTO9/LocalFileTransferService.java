package com.dmsBackend.ArchiveWithLTO9;

import com.dmsBackend.entity.ActionTypeForReport;
import com.dmsBackend.entity.DocumentDetails;
import com.dmsBackend.entity.RetentionPolicy;
import com.dmsBackend.repository.DocumentDetailsRepository;
import com.dmsBackend.repository.EmployeeRepository;
import com.dmsBackend.repository.RetentionPolicyRepository;
import com.dmsBackend.response.CartridgeInfo;
import com.dmsBackend.service.DocumentActivityReportService;
import com.dmsBackend.utils.DetectCurrCartridge;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@Slf4j
public class LocalFileTransferService {

    @Value("${document.storage.path}")
    private String acStoragePath;

    @Value("${document.storage.base.path}")
    private String baseStoragePath;

    @Value("${current.ltfs.drive}")
    private String currentLtfsDrive;

    @Value("${lto.delete.mode}")
    private String deleteMode;

    @Value("${lto.delete.after.days}")
    private int deleteAfterDays;

    @Autowired
    private RetentionPolicyRepository retentionPolicyRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DocumentDetailsRepository detailRepo;

    @Autowired
    private DocumentActivityReportService documentActivityReportService;


//    public void archive(DocumentDetails detail, Long job) {
//
//        CartridgeInfo cartridgeInfo = DetectCurrCartridge.detect(currentLtfsDrive);
//
//        log.info("Current cartridge info: {}", cartridgeInfo != null ? cartridgeInfo.getCartridge() : "NULL");
//
//        try {
//
//            // -------- LTO MOUNT CHECK --------
//            Path tape = Paths.get(currentLtfsDrive);
//
//            if (!Files.exists(tape)) {
//                throw new RuntimeException("LTO mount not available");
//            }
//
//            detail.setLtoStatus("IN_PROGRESS");
//            detailRepo.save(detail);
//
//            String dbPath = detail.getPath();
//
//            Path source = Paths.get(dbPath);
//
//            if (!source.isAbsolute()) {
//                source = Paths.get(acStoragePath).resolve(dbPath).normalize();
//            }
//
//            if (!Files.exists(source)) {
//                throw new RuntimeException("Source file not found: " + source);
//            }
//
//            Path basePath = Paths.get(baseStoragePath).normalize();
//            Path relativePath = basePath.relativize(source);
//
//            String LTO_ROOT = currentLtfsDrive + "/FTP";
//
//            Path target = Paths.get(LTO_ROOT)
//                    .normalize()
//                    .resolve(relativePath)
//                    .normalize();
//
//            Files.createDirectories(target.getParent());
//
//            // -------- TAPE CAPACITY CHECK --------
////            checkTapeCapacity(source);
//
//            // -------- COPY FILE --------
//            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
//
//            // -------- CHECKSUM VERIFY --------
//            String sourceHash = checksum(source);
//            String targetHash = checksum(target);
//
//            if (!sourceHash.equals(targetHash)) {
//                throw new RuntimeException("Checksum verification failed");
//            }
//
//            detail.setLtoChecksum(sourceHash);
//
//            // -------- DB FLAGS --------
//            detail.setArchivedPath(target.toString());
//            detail.setLtoArchived(true);
//            detail.setArchive(true);
//            detail.setLtoArchivedOn(LocalDateTime.now());
//            detail.setLtoStatus("ARCHIVED");
//            detail.setLtoFailureReason(null);
//            detail.setCartridgeId(cartridgeInfo != null ? cartridgeInfo.getCartridge() : null);
//            detail.setLtoJobId(String.valueOf(job));
//
//            log.info("✅ Archived COPY → {}", target);
//
//            // -------- DELETE SOURCE --------
//            if (Files.exists(source)) {
//                deleteSource(source);
//                log.info("✅ Deleted source file: {}", source);
//            }
//
//            // -------- POLICY UPDATE --------
//            RetentionPolicy policy = retentionPolicyRepository.findById(job).orElse(null);
//
//            if (policy != null) {
//                policy.setArchiveStatus(LtoRetentionJob.JobStatus.COMPLETED.name());
//            }
//
//            // -------- ACTIVITY LOG --------
//            if (policy != null) {
//                documentActivityReportService.logAction(
//                        detail.getDocumentHeader(),
//                        detail,
//                        ActionTypeForReport.ARCHIVE,
//                        "REQUESTED",
//                        employeeRepository.findById(policy.getCreatedBy()).orElse(null),
//                        null,
//                        Map.of("location", target.toString())
//                );
//            }
//
//            detailRepo.save(detail);
//
//        } catch (Exception ex) {
//
//            log.error("❌ Archive failed for Detail ID={}", detail.getId(), ex);
//
//            if (ex.getMessage() != null && ex.getMessage().contains("LTO Tape Full")) {
//                log.error("❌ Tape Full detected. Archival paused.");
//            }
//
//            detail.setLtoStatus("FAILED");
//            detail.setLtoFailureReason(ex.getMessage());
//
//            detailRepo.save(detail);
//
//            throw new RuntimeException(ex.getMessage(), ex);
//        }
//    }


    private void checkTapeCapacity(Path source) {

        try {

            Path tape = Paths.get(currentLtfsDrive + "\\");

            if (!Files.exists(tape)) {
                throw new RuntimeException("LTO mount not available");
            }

            FileStore store = Files.getFileStore(tape);

            long usable = store.getUsableSpace();
            long fileSize = Files.size(source);

            if (fileSize > usable) {
                throw new RuntimeException("LTO Tape Full");
            }

        } catch (Exception ex) {
            throw new RuntimeException("Tape capacity check failed", ex);
        }
    }


    private String checksum(Path file) {

        try {

            MessageDigest md = MessageDigest.getInstance("SHA-256");

            try (InputStream is = Files.newInputStream(file)) {

                byte[] buffer = new byte[8192];
                int read;

                while ((read = is.read(buffer)) > 0) {
                    md.update(buffer, 0, read);
                }
            }

            byte[] hash = md.digest();

            StringBuilder sb = new StringBuilder();

            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();

        } catch (Exception ex) {
            throw new RuntimeException("Checksum calculation failed", ex);
        }
    }


    private void deleteSource(Path source) {

        try {

            if ("NEVER".equalsIgnoreCase(deleteMode)) {
                return;
            }

            if ("AFTER_TRANSFER".equalsIgnoreCase(deleteMode)) {
                Files.deleteIfExists(source);
            }

            if ("AFTER_DAYS".equalsIgnoreCase(deleteMode)) {
                log.info("Deletion scheduled after {} days", deleteAfterDays);
            }

        } catch (Exception ex) {
            log.error("Source delete failed {}", source, ex);
        }
    }


    public void archive(DocumentDetails detail, Long job) {

        log.info("🚀 ===== ARCHIVE START =====");
        log.info("📌 JobId={}", job);
        log.info("📌 DetailId={}", detail.getId());

        CartridgeInfo cartridgeInfo = DetectCurrCartridge.detect(currentLtfsDrive);

        log.info("🧩 Cartridge detection completed");
        log.info("📦 CartridgeInfo = {}", cartridgeInfo);
        log.info("📦 Cartridge = {}", cartridgeInfo != null ? cartridgeInfo.getCartridge() : "NULL");

        try {

            // -------- LTO MOUNT CHECK --------
            log.info("🔍 Checking LTO mount path: {}", currentLtfsDrive);

            Path tape = Paths.get(currentLtfsDrive);

            boolean tapeExists = Files.exists(tape);
            log.info("📁 Tape exists = {}", tapeExists);

            if (!tapeExists) {
                log.error("❌ LTO mount not available");
                throw new RuntimeException("LTO mount not available");
            }

            log.info("🟢 LTO mount OK");

            detail.setLtoStatus("IN_PROGRESS");
            detailRepo.save(detail);
            log.info("💾 Detail status set to IN_PROGRESS");

            // -------- SOURCE FILE --------
            String dbPath = detail.getPath();
            log.info("📄 DB Path = {}", dbPath);

            Path source = Paths.get(dbPath);

            if (!source.isAbsolute()) {
                source = Paths.get(acStoragePath).resolve(dbPath).normalize();
            }

            log.info("📂 Resolved source path = {}", source);

            boolean sourceExists = Files.exists(source);
            log.info("📁 Source exists = {}", sourceExists);

            if (!sourceExists) {
                log.error("❌ Source file not found: {}", source);
                throw new RuntimeException("Source file not found: " + source);
            }

            // -------- TARGET PATH --------
            Path basePath = Paths.get(baseStoragePath).normalize();
            Path relativePath = basePath.relativize(source);

            String LTO_ROOT = currentLtfsDrive + "/FTP";

            Path target = Paths.get(LTO_ROOT)
                    .normalize()
                    .resolve(relativePath)
                    .normalize();

            log.info("🎯 Target path = {}", target);

            Files.createDirectories(target.getParent());
            log.info("📁 Target directories ensured");

            // -------- COPY --------
            log.info("📤 Starting file copy...");
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            log.info("✅ File copied successfully");

            // -------- CHECKSUM --------
            log.info("🔐 Calculating checksum...");
            String sourceHash = checksum(source);
            String targetHash = checksum(target);

            log.info("🔐 Source hash = {}", sourceHash);
            log.info("🔐 Target hash = {}", targetHash);

            if (!sourceHash.equals(targetHash)) {
                log.error("❌ Checksum mismatch!");
                throw new RuntimeException("Checksum verification failed");
            }

            log.info("✅ Checksum verified");

            detail.setLtoChecksum(sourceHash);

            // -------- DB FLAGS --------
            log.info("📝 Updating DB fields...");

            detail.setArchivedPath(target.toString());
            detail.setLtoArchived(true);
            detail.setArchive(true);
            detail.setLtoArchivedOn(LocalDateTime.now());
            detail.setLtoStatus("ARCHIVED");
            detail.setLtoFailureReason(null);
            detail.setCartridgeId(cartridgeInfo != null ? cartridgeInfo.getCartridge() : null);
            detail.setLtoJobId(String.valueOf(job));

            log.info("💾 Saving DocumentDetails...");
            detailRepo.save(detail);

            log.info("📌 DocumentDetails saved");

            // -------- POLICY UPDATE --------
            log.info("🔍 Fetching RetentionPolicy for jobId={}", job);

            RetentionPolicy policy = retentionPolicyRepository.findById(job).orElse(null);

            if (policy == null) {
                log.warn("⚠️ RetentionPolicy NOT FOUND for jobId={}", job);
            } else {
                log.info("✅ RetentionPolicy found: id={}, status={}",
                        policy.getId(), policy.getArchiveStatus());

                log.info("🔄 Setting archiveStatus = COMPLETED");

                policy.setArchiveStatus(LtoRetentionJob.JobStatus.COMPLETED.name());

                log.info("✔️ Status set in memory = {}", policy.getArchiveStatus());

                retentionPolicyRepository.save(policy);

                log.info("💾 RetentionPolicy saved");
            }

            // -------- ACTIVITY LOG --------
            if (policy != null) {
                log.info("📊 Logging activity report...");

                documentActivityReportService.logAction(
                        detail.getDocumentHeader(),
                        detail,
                        ActionTypeForReport.ARCHIVE,
                        "REQUESTED",
                        employeeRepository.findById(policy.getCreatedBy()).orElse(null),
                        null,
                        Map.of("location", target.toString())
                );

                log.info("📊 Activity logged");
            }

            log.info("🏁 ===== ARCHIVE SUCCESS END =====");

        } catch (Exception ex) {

            log.error("❌ ARCHIVE FAILED for DetailId={}", detail.getId(), ex);

            detail.setLtoStatus("FAILED");
            detail.setLtoFailureReason(ex.getMessage());

            detailRepo.save(detail);

            log.error("💾 Failure status saved to DB");

            throw new RuntimeException(ex.getMessage(), ex);
        }
    }


}