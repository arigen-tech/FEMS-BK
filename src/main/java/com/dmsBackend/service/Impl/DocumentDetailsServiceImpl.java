package com.dmsBackend.service.Impl;

import com.dmsBackend.ArchiveCodes.ArchiveJob;
import com.dmsBackend.entity.*;
import com.dmsBackend.exception.ResourceNotFoundException;
import com.dmsBackend.repository.*;
import com.dmsBackend.response.*;
import com.dmsBackend.service.*;
import com.dmsBackend.utils.*;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.CipherInputStream;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.sql.Timestamp;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import java.io.ByteArrayInputStream;


@Slf4j
@Service
public class DocumentDetailsServiceImpl implements DocumentDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentDetailsServiceImpl.class);
    private static final Pattern VERSION_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)(?:\\.(\\d+))?$");
    private final DocumentDetailsRepository documentDetailsRepository;
    private final DocumentHeaderRepository documentHeaderRepository;

    @Autowired
    private  DocumentActivityReportService documentActivityReportService;

    private final CurrentUser currentUser;
    @Autowired
    private NotificationService notificationService;

    @Autowired
    private FilesTypeMasterRepository filesTypeMasterRepository;

    @Autowired
    AuditLogUtil auditLogUtil;
    @Autowired
    private FileEncryptionUtil fileEncryptionUtil;

    @Value("${document.storage.path}")
    private String documentStoragePath;

    @Value("${document.archive.path}")
    private String documentArchivePath;
    @Value("${waitingroom.storage.path}")
    private String waitingRoomStoragePath;
    @Autowired
    private WaitingRoomScheduler waitingRoomScheduler;
    @Autowired
    private WaitingRoomRepository waitingRoomRepository;


    @Value("${document.sftp.host}") private String sftpHost;
    @Value("${document.sftp.port}") private int sftpPort;
    @Value("${document.sftp.username}") private String sftpUser;
    @Value("${document.sftp.password}") private String sftpPassword;
    @Value("${document.sftp.baseDir}") private String sftpBaseDir;


    @Value("${file.max.filesize}")
    private long filesizeUploads;

    private final CategoryMasterRepository categoryMasterRepository;
    private final YearMasterRepository yearMasterRepository;
    private final DocumentsAuditLogService documentsAuditLogService;

    private final DocHeaderStatusService docHeaderStatusService;

    private final DocHelper docHelper;






    public DocumentDetailsServiceImpl(DocumentDetailsRepository documentDetailsRepository, DocumentHeaderRepository documentHeaderRepository, DocHelper docHelper,
                                      CategoryMasterRepository categoryMasterRepository,CurrentUser currentUser , DocumentsAuditLogService documentsAuditLogService,YearMasterRepository yearMasterRepository, DocHeaderStatusService docHeaderStatusService) {
        this.documentDetailsRepository = documentDetailsRepository;
        this.documentHeaderRepository = documentHeaderRepository;
        this.categoryMasterRepository = categoryMasterRepository;
        this.docHelper = docHelper;
        this.yearMasterRepository=yearMasterRepository;
        this.documentsAuditLogService=documentsAuditLogService;
        this.docHeaderStatusService=docHeaderStatusService;
        this.currentUser=currentUser;
    }

    private final Tika tika = new Tika();




    private boolean isValidSemanticVersion(String version) {
        if (version == null || version.isEmpty()) {
            return false;
        }
        return VERSION_PATTERN.matcher(version).matches();
    }



    private int[] parseVersion(String version) {
        Matcher matcher = VERSION_PATTERN.matcher(version);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid version format: " + version);
        }

        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        int patch = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;

        return new int[]{major, minor, patch};
    }


    private String formatVersion(int major, int minor, int patch) {
        return String.format("%d.%d.%d", major, minor, patch);
    }

    private int compareVersions(String v1, String v2) {
        int[] v1Parts = parseVersion(v1);
        int[] v2Parts = parseVersion(v2);

        if (v1Parts[0] != v2Parts[0]) {
            return Integer.compare(v1Parts[0], v2Parts[0]);
        }
        if (v1Parts[1] != v2Parts[1]) {
            return Integer.compare(v1Parts[1], v2Parts[1]);
        }
        return Integer.compare(v1Parts[2], v2Parts[2]);
    }





//    @Transactional
//    @Override
//    public Map<String, Object> uploadFiles(List<MultipartFile> files,
//                                           String branch, String department, String category,
//                                           String year, String version) {
//
//        // Sanitize path segments
//        branch = sanitizeSegment(branch);
//        department = sanitizeSegment(department);
//        category = sanitizeSegment(category);
//        year = sanitizeSegment(year);
//        version = sanitizeSegment(version);
//
//        String relativeDir = String.format("%s/%s/%s/%s/%s", branch, department, year, category, version);
//
//        Path root = Path.of(documentStoragePath).normalize().toAbsolutePath();
//        Path targetDir = root.resolve(relativeDir).normalize();
//        if (!targetDir.startsWith(root)) {
//            throw new SecurityException("Invalid storage path.");
//        }
//
//        try {
//            Files.createDirectories(targetDir);
//        } catch (IOException e) {
//            throw new RuntimeException("Failed to create directory: " + targetDir, e);
//        }
//
//        final long MAX_FILE_BYTES = 100L * 1024 * 1024; // 100 MB
//        List<String> activeExt = filesTypeMasterRepository.findActiveFileExtensions(); // e.g. [".pdf",".docx"]
//        Set<String> allowed = activeExt.stream().map(String::toLowerCase).collect(Collectors.toSet());
//
//        List<Map<String, String>> uploaded = new CopyOnWriteArrayList<>();
//        List<Map<String, String>> errors = new CopyOnWriteArrayList<>();
//
//        // Thread pool capped to avoid IO thrash
//        int threads = Math.max(2, Math.min(Runtime.getRuntime().availableProcessors(), 8));
//        ExecutorService pool = Executors.newFixedThreadPool(threads);
//        List<Future<?>> futures = new ArrayList<>();
//
//        for (MultipartFile f : files) {
//            if (f == null || f.isEmpty()) continue;
//
//            futures.add(pool.submit(() -> {
//                String original = f.getOriginalFilename() != null ? f.getOriginalFilename() : "unknown";
//                try {
//                    // Size check
//                    if (f.getSize() > MAX_FILE_BYTES) {
//                        errors.add(Map.of("file", original, "error", "File too large"));
//                        return;
//                    }
//
//                    // Extension check
//                    String ext = extOf(original).toLowerCase();
//                    if (!allowed.contains(ext)) {
//                        errors.add(Map.of("file", original, "error", "Unsupported file type"));
//                        return;
//                    }
//
//                    // Basic MIME sniff (optional, cheap)
//                    String contentType = f.getContentType() != null ? f.getContentType() : "application/octet-stream";
//
//                    // Sanitize file name & ensure uniqueness
//                    String safeName = System.currentTimeMillis() + "_" +
//                            original.replaceAll("[^a-zA-Z0-9._-]", "_");
//
//                    Path dest = targetDir.resolve(safeName).normalize();
//                    if (!dest.startsWith(root)) {
//                        errors.add(Map.of("file", original, "error", "Invalid destination path"));
//                        return;
//                    }
//
//                    // Save
//                    try (InputStream in = f.getInputStream()) {
//                        Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
//                    }
//
//                    String relPath = root.relativize(dest).toString().replace("\\", "/");
//                    uploaded.add(Map.of(
//                            "originalName", original,
//                            "storedName", safeName,
//                            "relativePath", relPath,
//                            "contentType", contentType));
//
//                } catch (IOException ex) {
//                    errors.add(Map.of("file", original, "error", ex.getMessage()));
//                }
//            }));
//        }
//
//        for (Future<?> fu : futures) {
//            try { fu.get(); }
//            catch (InterruptedException ie) { Thread.currentThread().interrupt(); errors.add(Map.of("file","unknown","error","Interrupted")); }
//            catch (ExecutionException ee) { errors.add(Map.of("file","unknown","error", ee.getCause()!=null?ee.getCause().getMessage():ee.getMessage())); }
//        }
//        pool.shutdown();
//
//        Map<String, Object> resp = new HashMap<>();
//        resp.put("uploadedFiles", uploaded);
//        resp.put("errors", errors);
//        return resp;
//    }
//
//    private static String sanitizeSegment(String s) {
//        if (s == null) return "NA";
//        String t = s.trim().replace(" ", "_").replaceAll("[^a-zA-Z0-9._-]", "_");
//        return t.isBlank() ? "NA" : t;
//    }
//    private static String extOf(String filename) {
//        int i = filename.lastIndexOf('.');
//        return (i >= 0) ? filename.substring(i) : "";
//    }


//    @Transactional
//    @Override
//    public Map<String, Object> uploadFiles(List<MultipartFile> files,
//                                           String branch, String department, String category,
//                                           String year, String version) {
//
//        // Sanitize path segments
//        branch = sanitizeSegment(branch);
//        department = sanitizeSegment(department);
//        category = sanitizeSegment(category);
//        year = sanitizeSegment(year);
//        version = sanitizeSegment(version);
//
//        // Build relative directory for remote upload
//        String relativeDir = String.format("%s/%s/%s/%s/%s", branch, department, year, category, version);
//
//        final long MAX_FILE_BYTES = 100L * 1024 * 1024; // 100 MB
//        List<String> activeExt = filesTypeMasterRepository.findActiveFileExtensions(); // e.g. [".pdf",".docx"]
//        Set<String> allowed = activeExt.stream().map(String::toLowerCase).collect(Collectors.toSet());
//
//        List<Map<String, String>> uploaded = new CopyOnWriteArrayList<>();
//        List<Map<String, String>> errors = new CopyOnWriteArrayList<>();
//
//        // Thread pool capped to avoid IO thrash
//        int threads = Math.max(2, Math.min(Runtime.getRuntime().availableProcessors(), 8));
//        ExecutorService pool = Executors.newFixedThreadPool(threads);
//        List<Future<?>> futures = new ArrayList<>();
//
//        for (MultipartFile f : files) {
//            if (f == null || f.isEmpty()) continue;
//
//            futures.add(pool.submit(() -> {
//                String original = f.getOriginalFilename() != null ? f.getOriginalFilename() : "unknown";
//                try {
//                    // Size check
//                    if (f.getSize() > MAX_FILE_BYTES) {
//                        errors.add(Map.of("file", original, "error", "File too large"));
//                        return;
//                    }
//
//                    // Extension check
//                    String ext = extOf(original).toLowerCase();
//                    if (!allowed.contains(ext)) {
//                        errors.add(Map.of("file", original, "error", "Unsupported file type"));
//                        return;
//                    }
//
//                    // Basic MIME sniff (optional, cheap)
//                    String contentType = f.getContentType() != null ? f.getContentType() : "application/octet-stream";
//
//                    // Sanitize file name & ensure uniqueness
////                    String safeName = System.currentTimeMillis() + "_" +
////                            original.replaceAll("[^a-zA-Z0-9._-]", "_");
//
//                    // Sanitize file name only (no timestamp)
//                    String safeName = original.replaceAll("[^a-zA-Z0-9._-]", "_");
//
//
//                    // Upload to remote EC2 using SFTP
//                    try (InputStream in = f.getInputStream()) {
//                        SftpUtil.upload(
//                                in,
//                                String.format("%s/%s", sftpBaseDir, relativeDir), // remote directory
//                                safeName,
//                                sftpHost,
//                                sftpPort,
//                                sftpUser,
//                                sftpPassword
//                        );
//                    }
//
//                    String relPath = relativeDir + "/" + safeName;
//                    uploaded.add(Map.of(
//                            "originalName", original,
//                            "storedName", safeName,
//                            "relativePath", relPath,
//                            "contentType", contentType));
//
//                } catch (Exception ex) {
//                    errors.add(Map.of("file", original, "error", ex.getMessage()));
//                }
//            }));
//        }
//
//        for (Future<?> fu : futures) {
//            try { fu.get(); }
//            catch (InterruptedException ie) {
//                Thread.currentThread().interrupt();
//                errors.add(Map.of("file","unknown","error","Interrupted"));
//            }
//            catch (ExecutionException ee) {
//                errors.add(Map.of("file","unknown","error",
//                        ee.getCause()!=null ? ee.getCause().getMessage() : ee.getMessage()));
//            }
//        }
//        pool.shutdown();
//
//        Map<String, Object> resp = new HashMap<>();
//        resp.put("uploadedFiles", uploaded);
//        resp.put("errors", errors);
//        return resp;
//    }

// WITHOUT ENCRYPTION
//    @Transactional
//    @Override
//    public Map<String, Object> uploadFiles(List<MultipartFile> files,
//                                           String branch, String department, String category,
//                                           String year, String version,
//                                           List<Integer> waitingRoomIds) {
//
//        // Sanitize path segments
//        branch = sanitizeSegment(branch);
//        department = sanitizeSegment(department);
//        category = sanitizeSegment(category);
//        year = sanitizeSegment(year);
//        version = sanitizeSegment(version);
//
//        // Build relative directory (without full path)
//        String relativeDir = String.format("%s/%s/%s/%s/%s", branch, department, year, category, version);
//
//        final long MAX_FILE_BYTES = filesizeUploads;
//        List<String> activeExt = filesTypeMasterRepository.findActiveFileExtensions();
//        Set<String> allowed = activeExt.stream().map(String::toLowerCase).collect(Collectors.toSet());
//
//        List<Map<String, Object>> uploaded = new CopyOnWriteArrayList<>();
//        List<Map<String, String>> errors = new CopyOnWriteArrayList<>();
//
//        int threads = Math.max(2, Math.min(Runtime.getRuntime().availableProcessors(), 8));
//        ExecutorService pool = Executors.newFixedThreadPool(threads);
//        List<Future<?>> futures = new ArrayList<>();
//
//        DocHelper docHelper = new DocHelper();
//
//        // ✅ FIRST: Move waiting room files before processing new uploads
//        if (waitingRoomIds != null && !waitingRoomIds.isEmpty()) {
//            List<Integer> successfullyMovedIds = new ArrayList<>();
//
//            for (Integer waitingRoomId : waitingRoomIds) {
//                try {
//                    WaitingRoom waitingRoomFile = waitingRoomRepository.findById(waitingRoomId)
//                            .orElseThrow(() -> new RuntimeException("Waiting room file not found: " + waitingRoomId));
//
//                    // Get the file name from the waiting room file path
//                    String waitingRoomFilePath = waitingRoomFile.getFilepath();
//                    String fileName = Paths.get(waitingRoomFilePath).getFileName().toString();
//
//                    // Source path in waiting room storage
//                    Path sourcePath = Paths.get(waitingRoomStoragePath, fileName);
//
//                    // Check if source file exists
//                    if (!Files.exists(sourcePath)) {
//                        throw new RuntimeException("Source file not found in waiting room: " + sourcePath);
//                    }
//
//                    // Target directory in document storage
//                    Path targetDir = Paths.get(documentStoragePath, relativeDir);
//                    Files.createDirectories(targetDir);
//
//                    // Target path in document storage
//                    Path targetPath = targetDir.resolve(fileName);
//
//                    // Move the file
//                    Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
//
//                    // Get file info after move
//                    long fileSize = Files.size(targetPath);
//                    String fileExtension = getFileExtension(fileName).toLowerCase();
//
//                    // Add to uploaded list
//                    Map<String, Object> fileMeta = new LinkedHashMap<>();
//                    fileMeta.put("originalName", waitingRoomFile.getDocumentName());
//                    fileMeta.put("storedName", fileName);
//                    fileMeta.put("path", relativeDir + "/" + fileName); // Relative path only
//                    fileMeta.put("contentType", Files.probeContentType(targetPath));
//                    fileMeta.put("fileSizeBytes", fileSize);
//                    fileMeta.put("fileSizeHuman", docHelper.humanReadableSize(fileSize));
//                    fileMeta.put("fileType", fileExtension);
//                    fileMeta.put("pageCount", docHelper.getPageCount(targetPath, fileExtension));
//                    fileMeta.put("waitingRoomId", waitingRoomId);
//                    fileMeta.put("isWaitingRoomFile", true);
//
//                    uploaded.add(fileMeta);
//                    successfullyMovedIds.add(waitingRoomId);
//
//                    log.info("Successfully moved waiting room file {} from {} to {}",
//                            waitingRoomId, sourcePath, targetPath);
//
//                } catch (Exception ex) {
//                    log.error("Failed to move waiting room file: {}", waitingRoomId, ex);
//                    errors.add(Map.of("waitingRoomId", waitingRoomId.toString(), "error", ex.getMessage()));
//                    // Mark as FAILED immediately if move fails
//                    waitingRoomScheduler.updateStatusToFailed(List.of(waitingRoomId));
//                }
//            }
//
//            // ✅ Update status to MOVED for successfully moved files
//            if (!successfullyMovedIds.isEmpty()) {
//                waitingRoomScheduler.updateStatusToMoved(successfullyMovedIds);
//                log.info("Updated {} waiting room files to MOVED status", successfullyMovedIds.size());
//            }
//        }
//
//        // ✅ SECOND: Process new file uploads (regular files)
//        for (MultipartFile f : files) {
//            if (f == null || f.isEmpty()) continue;
//
//            futures.add(pool.submit(() -> {
//                String original = f.getOriginalFilename() != null ? f.getOriginalFilename() : "unknown";
//                try {
//                    // Size check
//                    if (f.getSize() > MAX_FILE_BYTES) {
//                        errors.add(Map.of("file", original, "error", "File too large"));
//                        return;
//                    }
//
//                    // Extension check
//                    String ext = extOf(original).toLowerCase();
//                    if (!allowed.contains(ext)) {
//                        errors.add(Map.of("file", original, "error", "Unsupported file type"));
//                        return;
//                    }
//
//                    // MIME type
//                    String contentType = f.getContentType() != null ? f.getContentType() : "application/octet-stream";
//
//                    // Sanitize file name
//                    String safeName = original.replaceAll("[^a-zA-Z0-9._-]", "_");
//
//                    // Directory path
//                    Path targetDir = Paths.get(documentStoragePath, relativeDir);
//                    Files.createDirectories(targetDir);
//
//                    // File path
//                    Path targetFile = targetDir.resolve(safeName);
//
//                    // Save locally
//                    try (InputStream in = f.getInputStream()) {
//                        Files.copy(in, targetFile, StandardCopyOption.REPLACE_EXISTING);
//                    }
//
//                    // Extract page/sheet/slide count
//                    int pageCount = docHelper.getPageCount(targetFile, ext);
//
//                    // File size formatting
//                    long sizeBytes = f.getSize();
//                    String humanSize = docHelper.humanReadableSize(sizeBytes);
//
//                    // Response entry - use relative path only
//                    String relPath = relativeDir + "/" + safeName;
//                    Map<String, Object> fileMeta = new LinkedHashMap<>();
//                    fileMeta.put("originalName", original);
//                    fileMeta.put("storedName", safeName);
//                    fileMeta.put("path", relPath);
//                    fileMeta.put("contentType", contentType);
//                    fileMeta.put("fileSizeBytes", sizeBytes);
//                    fileMeta.put("fileSizeHuman", humanSize);
//                    fileMeta.put("fileType", ext);
//                    fileMeta.put("pageCount", pageCount);
//
//                    uploaded.add(fileMeta);
//
//                } catch (Exception ex) {
//                    errors.add(Map.of("file", original, "error", ex.getMessage()));
//                }
//            }));
//        }
//
//        // Wait for all upload tasks to complete
//        for (Future<?> fu : futures) {
//            try {
//                fu.get();
//            } catch (InterruptedException ie) {
//                Thread.currentThread().interrupt();
//                errors.add(Map.of("file", "unknown", "error", "Interrupted"));
//            } catch (ExecutionException ee) {
//                errors.add(Map.of("file", "unknown", "error",
//                        ee.getCause() != null ? ee.getCause().getMessage() : ee.getMessage()));
//            }
//        }
//        pool.shutdown();
//
//        Map<String, Object> resp = new HashMap<>();
//        resp.put("uploadedFiles", uploaded);
//        resp.put("errors", errors);
//        return resp;
//    }


    @Transactional
    @Override
    public Map<String, Object> uploadFiles(List<MultipartFile> files,
                                           String branch, String department, String category,
                                           String year, String version,
                                           List<Integer> waitingRoomIds) {

        log.info("API CALL → Upload Files | branch={} department={} category={} year={} version={} waitingRoomIds={}",
                branch, department, category, year, version, waitingRoomIds);

        branch = sanitizeSegment(branch);
        department = sanitizeSegment(department);
        category = sanitizeSegment(category);
        year = sanitizeSegment(year);
        version = sanitizeSegment(version);

        String relativeDir = String.format("%s/%s/%s/%s/%s",
                branch, department, year, category, version);

        final long MAX_FILE_BYTES = filesizeUploads;
        List<String> activeExt = filesTypeMasterRepository.findActiveFileExtensions();
        Set<String> allowed = activeExt.stream().map(String::toLowerCase).collect(Collectors.toSet());

        List<Map<String, Object>> uploaded = new CopyOnWriteArrayList<>();
        List<Map<String, String>> errors = new CopyOnWriteArrayList<>();

        int threads = Math.max(2, Math.min(Runtime.getRuntime().availableProcessors(), 8));
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Future<?>> futures = new ArrayList<>();

        DocHelper docHelper = new DocHelper();

        /* ==========================================================
           1️⃣ WAITING ROOM FILES → MOVE + ENCRYPT (SAME NAME)
           ========================================================== */
        if (waitingRoomIds != null && !waitingRoomIds.isEmpty()) {
            log.info("Processing {} waiting room files", waitingRoomIds.size());

            List<Integer> movedIds = new ArrayList<>();

            for (Integer waitingRoomId : waitingRoomIds) {
                try {
                    log.debug("Processing waiting room file: {}", waitingRoomId);

                    WaitingRoom wr = waitingRoomRepository.findById(waitingRoomId)
                            .orElseThrow(() -> {
                                log.error("Waiting room file not found | waitingRoomId={}", waitingRoomId);
                                return new RuntimeException("Waiting room file not found: " + waitingRoomId);
                            });

                    String originalFileName = Paths.get(wr.getFilepath()).getFileName().toString();

                    Path sourcePath = Paths.get(waitingRoomStoragePath, originalFileName);

                    if (!Files.exists(sourcePath)) {
                        log.error("Waiting room file missing | waitingRoomId={} path={}", waitingRoomId, sourcePath);
                        throw new RuntimeException("Waiting room file missing: " + sourcePath);
                    }

                    String ext = getFileExtension(originalFileName).toLowerCase();

                    // ✅ Page count BEFORE encryption
                    log.debug("Getting page count for waiting room file: {}", originalFileName);
                    int pageCount = docHelper.getPageCount(sourcePath, ext);

                    Path targetDir = Paths.get(documentStoragePath, relativeDir);
                    Files.createDirectories(targetDir);

                    // 🔐 Save encrypted file with SAME filename
                    Path encryptedTarget = targetDir.resolve(originalFileName);

                    log.debug("Encrypting and moving file: {} to {}", sourcePath, encryptedTarget);
                    try (
                            InputStream in = Files.newInputStream(sourcePath);
                            OutputStream out = Files.newOutputStream(
                                    encryptedTarget,
                                    StandardOpenOption.CREATE,
                                    StandardOpenOption.TRUNCATE_EXISTING)
                    ) {
                        fileEncryptionUtil.encrypt(in, out);
                    }

                    Files.delete(sourcePath);

                    long size = Files.size(encryptedTarget);

                    Map<String, Object> meta = new LinkedHashMap<>();
                    meta.put("originalName", wr.getDocumentName());
                    meta.put("storedName", originalFileName);
                    meta.put("path", relativeDir + "/" + originalFileName);
                    meta.put("fileSizeBytes", size);
                    meta.put("fileSizeHuman", docHelper.humanReadableSize(size));
                    meta.put("fileType", ext);
                    meta.put("pageCount", pageCount);
                    meta.put("waitingRoomId", waitingRoomId);
                    meta.put("isWaitingRoomFile", true);

                    uploaded.add(meta);
                    movedIds.add(waitingRoomId);

                    log.info("SUCCESS → Moved waiting room file | waitingRoomId={} fileName={} size={} pages={}",
                            waitingRoomId, originalFileName, docHelper.humanReadableSize(size), pageCount);

                } catch (Exception ex) {
                    log.error("FAILED → Move waiting room file | waitingRoomId={} reason={}",
                            waitingRoomId, ex.getMessage(), ex);

                    errors.add(Map.of(
                            "waitingRoomId", waitingRoomId.toString(),
                            "error", ex.getMessage()
                    ));
                    waitingRoomScheduler.updateStatusToFailed(List.of(waitingRoomId));
                }
            }

            if (!movedIds.isEmpty()) {
                log.info("Updating {} waiting room files to MOVED status", movedIds.size());
                waitingRoomScheduler.updateStatusToMoved(movedIds);
            }
        }

        /* ==========================================================
           2️⃣ NEW FILE UPLOADS → SAVE + ENCRYPT (SAME NAME)
           ========================================================== */
        log.info("Processing {} new file uploads", files.size());

        for (MultipartFile f : files) {
            if (f == null || f.isEmpty()) continue;

            futures.add(pool.submit(() -> {
                String original = Optional.ofNullable(f.getOriginalFilename()).orElse("unknown");

                try {
                    log.debug("Processing file upload: {}", original);

                    // Size check
                    if (f.getSize() > MAX_FILE_BYTES) {
                        log.warn("File too large | fileName={} size={} maxAllowed={}",
                                original, f.getSize(), MAX_FILE_BYTES);
                        errors.add(Map.of("file", original, "error", "File too large"));
                        return;
                    }

                    // Extension check
                    String ext = extOf(original).toLowerCase();
                    if (!allowed.contains(ext)) {
                        log.warn("Unsupported file type | fileName={} extension={} allowedTypes={}",
                                original, ext, allowed);
                        errors.add(Map.of("file", original, "error", "Unsupported file type"));
                        return;
                    }

                    String safeName = original.replaceAll("[^a-zA-Z0-9._-]", "_");
                    Path targetDir = Paths.get(documentStoragePath, relativeDir);
                    Files.createDirectories(targetDir);

                    // temp file only for page count
                    Path tempFile = Files.createTempFile("upload-", "." + ext);
                    try (InputStream in = f.getInputStream()) {
                        Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
                    }

                    int pageCount = docHelper.getPageCount(tempFile, ext);

                    // 🔐 Save encrypted file with SAME filename
                    Path encryptedFile = targetDir.resolve(safeName);
                    log.debug("Encrypting and saving file: {} to {}", original, encryptedFile);

                    try (
                            InputStream in = Files.newInputStream(tempFile);
                            OutputStream out = Files.newOutputStream(
                                    encryptedFile,
                                    StandardOpenOption.CREATE,
                                    StandardOpenOption.TRUNCATE_EXISTING)
                    ) {
                        fileEncryptionUtil.encrypt(in, out);
                    }

                    Files.deleteIfExists(tempFile);

                    long size = Files.size(encryptedFile);

                    Map<String, Object> meta = new LinkedHashMap<>();
                    meta.put("originalName", original);
                    meta.put("storedName", safeName);
                    meta.put("path", relativeDir + "/" + safeName);
                    meta.put("fileSizeBytes", size);
                    meta.put("fileSizeHuman", docHelper.humanReadableSize(size));
                    meta.put("fileType", ext);
                    meta.put("pageCount", pageCount);

                    uploaded.add(meta);

                    log.info("SUCCESS → Uploaded file | fileName={} size={} pages={}",
                            original, docHelper.humanReadableSize(size), pageCount);

                } catch (Exception ex) {
                    log.error("FAILED → Upload file | fileName={} reason={}",
                            original, ex.getMessage(), ex);
                    errors.add(Map.of("file", original, "error", ex.getMessage()));
                }
            }));
        }

        // Wait for all upload tasks to complete
        for (Future<?> f : futures) {
            try {
                f.get();
            } catch (Exception e) {
                log.error("FAILED → Upload task execution | reason={}", e.getMessage(), e);
                errors.add(Map.of("file", "unknown", "error", e.getMessage()));
            }
        }
        pool.shutdown();

        Map<String, Object> resp = new HashMap<>();
        resp.put("uploadedFiles", uploaded);
        resp.put("errors", errors);

        log.info("SUCCESS → File upload completed | successful={} failed={}",
                uploaded.size(), errors.size());

        return resp;
    }


    // Helper method to get file extension
    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        return (lastDotIndex > 0) ? fileName.substring(lastDotIndex + 1) : "";
    }





    // ---------- Helpers ----------
    private static String sanitizeSegment(String s) {
        if (s == null) return "NA";
        String t = s.trim().replace(" ", "_").replaceAll("[^a-zA-Z0-9._-]", "_");
        return t.isBlank() ? "NA" : t;
    }

    private static String extOf(String filename) {
        int i = filename.lastIndexOf('.');
        return (i >= 0) ? filename.substring(i) : "";
    }

    private String humanReadableSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char unit = "KMGTPE".charAt(exp - 1);
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), unit);
    }



    @Override
    public void saveFileDetails(DocumentHeader header, List<DocumentSaveRequest.FilePathVersion> files, String actor) {
        log.info("API CALL → Save File Details | headerId={} fileCount={} actor={}",
                header.getId(), files.size(), actor);

        Timestamp now = new Timestamp(System.currentTimeMillis());
        for (DocumentSaveRequest.FilePathVersion fpv : files) {
            try {
                DocumentDetails d = new DocumentDetails();

                String fileName = fpv.getPath().substring(fpv.getPath().lastIndexOf('/') + 1);
                d.setDocName(fileName);
                d.setPath(fpv.getPath());
                d.setVersion(fpv.getVersion());
                d.setDocumentHeader(header);
                d.setStatus(DocApprovalStatus.PENDING); // default
                d.setMimeType(fpv.getMimeType());
                d.setFileType(fpv.getFileType());
                d.setFileSizeBytes(fpv.getFileSizeBytes());
                d.setFileSizeHuman(fpv.getFileSizeHuman());
                d.setPageCounts(fpv.getPageCounts());
                d.setCreatedOn(now);
                d.setUpdatedOn(now);
                d.setCreatedBy(actor);
                d.setUpdatedBy(actor);

                YearMaster ym = yearMasterRepository.findById(Math.toIntExact(fpv.getYearId()))
                        .orElseThrow(() -> {
                            log.error("Year not found | yearId={}", fpv.getYearId());
                            return new ResourceNotFoundException("Year not found: " + fpv.getYearId());
                        });
                d.setYearMaster(ym);

                DocumentDetails newDoc = documentDetailsRepository.save(d);
                log.debug("Saved file detail | fileName={} year={} version={}",
                        fileName, ym.getName(), fpv.getVersion());

                documentActivityReportService.logAction(
                        header,
                        newDoc,
                        ActionTypeForReport.UPLOAD,
                        "SUCCESS",
                        currentUser.getCurrentEmployeeOrThrow(),
                        null,
                        Map.of(
                                "version", newDoc.getVersion(),
                                "fileSize", newDoc.getFileSizeBytes()
                        )
                );

            } catch (Exception e) {
                log.error("FAILED → Save file detail | path={} reason={}",
                        fpv.getPath(), e.getMessage(), e);
                throw e;
            }
        }

        log.info("SUCCESS → Saved {} file details for header {}", files.size(), header.getId());
    }

    @Override
    @Transactional
    public void updateDetailStatus(Integer detailId, DocApprovalStatus newStatus, String reason, HttpServletRequest request) {
        log.info("API CALL → Update Detail Status | detailId={} newStatus={}", detailId, newStatus);

        Employee empObj = currentUser.getCurrentEmployeeOrThrow();

        DocumentDetails d = documentDetailsRepository.findById(detailId)
                .orElseThrow(() -> {
                    log.error("Document detail not found | detailId={}", detailId);
                    return new ResourceNotFoundException("Detail not found");
                });

        DocApprovalStatus old = d.getStatus();
        if (old == newStatus) {
            log.warn("Status unchanged | detailId={} currentStatus={}", detailId, old);
            return;
        }

        try {
            log.debug("Updating detail status | detailId={} oldStatus={} newStatus={}",
                    detailId, old, newStatus);

            // Update document detail
            d.setStatus(newStatus);
            d.setUpdatedOn(new Timestamp(System.currentTimeMillis()));
            d.setUpdatedBy(empObj.getEmail());

            if (newStatus == DocApprovalStatus.APPROVED) {
                d.setApprovedOn(new Timestamp(System.currentTimeMillis()));
                d.setApprovedBy(empObj.getEmail());
                d.setRejectionReason(null);
            } else if (newStatus == DocApprovalStatus.REJECTED) {
                d.setApprovedOn(null);
                d.setApprovedBy(null);
                d.setRejectionReason(reason);
            } else {
                d.setApprovedOn(null);
                d.setApprovedBy(null);
                d.setRejectionReason(null);
            }

            DocumentDetails newDoc = documentDetailsRepository.save(d);

            // Create a notification
            notificationService.createDocumentNotification(d);

            // Recalculate header status
            DocumentHeader header = d.getDocumentHeader();
            docHeaderStatusService.recalcAndUpdateHeaderStatus(header, empObj.getEmail());

            // Map entity to proper response object for audit
            DocumentDetailsResponse fileDetail = new DocumentDetailsResponse();
            fileDetail.setId(d.getId());
            fileDetail.setYear(d.getYearMaster() != null ? d.getYearMaster().getName() : null);
            fileDetail.setVersion(d.getVersion());
            fileDetail.setDocName(d.getDocName());

            // Build audit details JSON
            Map<String, Object> detailsJson = new HashMap<>();
            detailsJson.put("oldStatus", old != null ? old.name() : null);
            detailsJson.put("newStatus", newStatus.name());
            detailsJson.put("reason", reason);
            detailsJson.put("title", header.getTitle());
            detailsJson.put("subject", header.getSubject());
            if (header.getCategoryMaster() != null) {
                detailsJson.put("category", header.getCategoryMaster().getName());
            }
            if (d.getYearMaster() != null && d.getYearMaster().getName() != null) {
                detailsJson.put("year", d.getYearMaster().getName());
            }
            if (d.getVersion() != null) {
                detailsJson.put("version", d.getVersion());
            }
            if (d.getDocName() != null) {
                detailsJson.put("fileName", d.getDocName());
            }

            // ✅ Log success audit
            auditLogUtil.logDocumentAction(
                    empObj,
                    "DocumentPage",
                    "StatusChange",
                    "Success",
                    header.getId(),
                    List.of(fileDetail),
                    detailsJson,
                    request
            );

            log.info("SUCCESS → Updated detail status | detailId={} oldStatus={} newStatus={}",
                    detailId, old, newStatus);


            documentActivityReportService.logAction(
                    header,
                    newDoc,
                    newStatus == DocApprovalStatus.APPROVED
                            ? ActionTypeForReport.APPROVE
                            : ActionTypeForReport.REJECT,
                    "SUCCESS",
                    currentUser.getCurrentEmployeeOrThrow(),
                    null,
                    Map.of(
                            "version", newDoc.getVersion(),
                            "fileSize", newDoc.getFileSizeBytes()
                    )
            );

        } catch (Exception ex) {
            log.error("FAILED → Update detail status | detailId={} reason={}",
                    detailId, ex.getMessage(), ex);

            // Build minimal audit details for failure
            Map<String, Object> detailsJson = new HashMap<>();
            detailsJson.put("oldStatus", old != null ? old.name() : null);
            detailsJson.put("attemptedStatus", newStatus != null ? newStatus.name() : null);
            detailsJson.put("reason", reason);
            detailsJson.put("errorMessage", ex.getMessage());

            // Log failure in audit
            auditLogUtil.logDocumentAction(
                    empObj,
                    "DocumentPage",
                    "StatusChange",
                    "Failed",
                    d.getDocumentHeader().getId(),
                    null, // no file details on failure
                    detailsJson,
                    request
            );

            // Rethrow to trigger rollback
            throw ex;
        }
    }


//    @Transactional
//    @Override
//    public List<DocumentDetails> updateFileDetails(
//            CategoryMaster categoryMaster,
//            YearMaster yearMaster, // fallback year if file.yearId is null
//            DocumentHeader documentHeader,
//            List<DocumentSaveRequest.FilePathVersion> filePaths,
//            String version,
//            boolean updatePaths) {
//
//        if (filePaths == null || filePaths.isEmpty()) {
//            throw new IllegalArgumentException("File paths must not be empty.");
//        }
//
//        List<DocumentDetails> oldFiles = documentDetailsRepository.findByDocumentHeaderId(documentHeader.getId());
//        List<DocumentDetails> updatedFiles = new ArrayList<>();
//
//        String branch = sanitizeSegment(documentHeader.getEmployee().getBranch().getName());
//        String department = sanitizeSegment(documentHeader.getEmployee().getDepartment().getName());
//        String category = sanitizeSegment(categoryMaster.getName());
//        String defaultYear = sanitizeSegment(yearMaster.getName());
//
//        // Map new files by filename
//        Map<String, DocumentSaveRequest.FilePathVersion> newFilesMap = filePaths.stream()
//                .collect(Collectors.toMap(
//                        f -> Paths.get(f.getPath()).getFileName().toString(),
//                        f -> f,
//                        (a, b) -> b
//                ));
//
//        // ✅ 1. Process existing DB files
//        for (DocumentDetails oldFile : oldFiles) {
//            DocumentSaveRequest.FilePathVersion newFile = newFilesMap.get(oldFile.getDocName());
//
//            if (newFile == null) {
//                // Case 4: File deleted
//                String oldRemotePath = sftpBaseDir + "/" + oldFile.getPath();
//                try {
//                    SftpUtil.delete(oldRemotePath, sftpHost, sftpPort, sftpUser, sftpPassword);
//                    log.info("🗑️ Deleted file: {}", oldRemotePath);
//                } catch (Exception e) {
//                    throw new RuntimeException("Failed to delete remote file: " + oldRemotePath, e);
//                }
//                documentDetailsRepository.delete(oldFile);
//                continue;
//            }
//
//            String newFileName = Paths.get(newFile.getPath()).getFileName().toString();
//
//            // Resolve year per file
//            YearMaster newYearMaster = yearMasterRepository.findById(Math.toIntExact(newFile.getYearId()))
//                    .orElseThrow(() -> new ResourceNotFoundException("YearMaster not found with id " + newFile.getYearId()));
//
//            // Regenerate path
//            String newRelativePath = generateNewPath(branch, department, newYearMaster, categoryMaster, newFile.getVersion(), newFileName);
//
//            boolean pathChanged = !oldFile.getPath().equals(newRelativePath);
//            boolean versionChanged = !oldFile.getVersion().equals(newFile.getVersion());
//            boolean yearChanged = !oldFile.getYearMaster().getId().equals(newFile.getYearId());
//
//            if (pathChanged || versionChanged || yearChanged) {
//                // Case 2: Move on SFTP
//                String oldRemotePath = sftpBaseDir + "/" + oldFile.getPath();
//                String newRemoteDir = sftpBaseDir + "/" + Paths.get(newRelativePath).getParent().toString();
//
//                try {
//                    SftpUtil.move(oldRemotePath, newRemoteDir, newFileName,
//                            sftpHost, sftpPort, sftpUser, sftpPassword);
//                    log.info("📂 Moved file: {} → {}", oldRemotePath, newRelativePath);
//                } catch (Exception e) {
//                    throw new RuntimeException("Failed to move file: " + oldRemotePath + " → " + newRelativePath, e);
//                }
//
//                oldFile.setPath(newRelativePath);
//                oldFile.setVersion(newFile.getVersion());
//                oldFile.setDocName(newFileName);
//                oldFile.setYearMaster(newYearMaster);
//            } else {
//                // Case 1: No change
//                log.info("✅ Unchanged file: {}", oldFile.getPath());
//            }
//
//            oldFile.setUpdatedOn(new Timestamp(System.currentTimeMillis()));
//            documentDetailsRepository.save(oldFile);
//            updatedFiles.add(oldFile);
//        }
//
//        // ✅ 2. Process new files
//        for (DocumentSaveRequest.FilePathVersion newFile : filePaths) {
//            String fileName = Paths.get(newFile.getPath()).getFileName().toString();
//
//            boolean existsInDb = updatedFiles.stream()
//                    .anyMatch(f -> f.getDocName().equals(fileName) && f.getVersion().equals(newFile.getVersion()));
//
//            if (!existsInDb) {
//                // Case 3: New file → only add DB entry
//                YearMaster newYearMaster = yearMasterRepository.findById(Math.toIntExact(newFile.getYearId()))
//                    .orElseThrow(() -> new ResourceNotFoundException("YearMaster not found with id " + newFile.getYearId()));
//
//                DocumentDetails documentDetails = new DocumentDetails();
//                documentDetails.setDocName(fileName);
//                documentDetails.setPath(newFile.getPath()); // use provided path (already on SFTP)
//                documentDetails.setVersion(newFile.getVersion());
//                documentDetails.setDocumentHeader(documentHeader);
//                documentDetails.setYearMaster(newYearMaster);
//                documentDetails.setCreatedOn(new Timestamp(System.currentTimeMillis()));
//
//                documentDetailsRepository.save(documentDetails);
//                updatedFiles.add(documentDetails);
//
//                log.info("➕ Added new file entry (DB only): {}", newFile.getPath());
//            }
//        }
//
//        return updatedFiles;
//    }


    @Transactional
    @Override
    public List<DocumentDetails> updateFileDetails(
            CategoryMaster categoryMaster,
            YearMaster yearMaster, // fallback year if file.yearId is null
            DocumentHeader documentHeader,
            List<DocumentSaveRequest.FilePathVersion> filePaths,
            String version,
            boolean updatePaths) {

        log.info("API CALL → Update File Details | headerId={} fileCount={} updatePaths={}",
                documentHeader.getId(), filePaths.size(), updatePaths);

        if (filePaths == null || filePaths.isEmpty()) {
            log.error("File paths must not be empty");
            throw new IllegalArgumentException("File paths must not be empty.");
        }

        List<DocumentDetails> oldFiles = documentDetailsRepository.findByDocumentHeaderId(documentHeader.getId());
        List<DocumentDetails> updatedFiles = new ArrayList<>();

        String branch = sanitizeSegment(documentHeader.getEmployee().getBranch().getName());
        String department = sanitizeSegment(documentHeader.getEmployee().getDepartment().getName());
        String category = sanitizeSegment(categoryMaster.getName());
        String defaultYear = sanitizeSegment(yearMaster.getName());

        // Map new files by filename
        Map<String, DocumentSaveRequest.FilePathVersion> newFilesMap = filePaths.stream()
                .collect(Collectors.toMap(
                        f -> Paths.get(f.getPath()).getFileName().toString(),
                        f -> f,
                        (a, b) -> b
                ));

        // ✅ 1. Process existing DB files
//        for (DocumentDetails oldFile : oldFiles) {
//
//            if (ArchiveJob.Status.ARCHIVED.name().equals(oldFile.getArchivalStatus())) {
//                log.info("📌 Skipping archived file: {}", oldFile.getPath());
//                updatedFiles.add(oldFile);
//                continue;
//            }
//
//            DocumentSaveRequest.FilePathVersion newFile = newFilesMap.get(oldFile.getDocName());
//
//            if (newFile == null) {
//                // Case 4: File deleted (local)
//                Path oldLocalPath = Paths.get(documentStoragePath, oldFile.getPath());
//                try {
//                    Files.deleteIfExists(oldLocalPath);
//                    log.info("🗑️ Deleted local file: {}", oldLocalPath);
//                } catch (Exception e) {
//                    throw new RuntimeException("Failed to delete local file: " + oldLocalPath, e);
//                }
//                documentDetailsRepository.delete(oldFile);
//                continue;
//            }
//
//            String newFileName = Paths.get(newFile.getPath()).getFileName().toString();
//
//            // Resolve year per file
//            YearMaster newYearMaster = yearMasterRepository.findById(Math.toIntExact(newFile.getYearId()))
//                    .orElseThrow(() -> new ResourceNotFoundException("YearMaster not found with id " + newFile.getYearId()));
//
//            // Regenerate path
//            String newRelativePath = generateNewPath(branch, department, newYearMaster, categoryMaster, newFile.getVersion(), newFileName);
//
//            boolean pathChanged = !oldFile.getPath().equals(newRelativePath);
//            boolean versionChanged = !oldFile.getVersion().equals(newFile.getVersion());
//            boolean yearChanged = !oldFile.getYearMaster().getId().equals(newFile.getYearId());
//
//            if (pathChanged || versionChanged || yearChanged) {
//                // Case 2: Move locally
//                Path oldLocalPath = Paths.get(documentStoragePath, oldFile.getPath());
//                Path newLocalDir = Paths.get(documentStoragePath, newRelativePath).getParent();
//
//                try {
//                    Files.createDirectories(newLocalDir); // ensure dir exists
//                    Files.move(oldLocalPath, newLocalDir.resolve(newFileName), StandardCopyOption.REPLACE_EXISTING);
//                    log.info("📂 Moved local file: {} → {}", oldLocalPath, newRelativePath);
//                } catch (Exception e) {
//                    throw new RuntimeException("Failed to move local file: " + oldLocalPath + " → " + newRelativePath, e);
//                }
//
//                oldFile.setPath(newRelativePath);
//                oldFile.setVersion(newFile.getVersion());
//                oldFile.setDocName(newFileName);
//                oldFile.setYearMaster(newYearMaster);
//            } else {
//                // Case 1: No change
//                log.info("✅ Unchanged file: {}", oldFile.getPath());
//            }
//
//            oldFile.setUpdatedOn(new Timestamp(System.currentTimeMillis()));
//            documentDetailsRepository.save(oldFile);
//            updatedFiles.add(oldFile);
//        }

        // ✅ 2. Process new files

        // ✅ 1. Process existing DB files
        for (DocumentDetails oldFile : oldFiles) {

            if (ArchiveJob.Status.ARCHIVED.name().equals(oldFile.getArchivalStatus())) {
                log.info("📌 Skipping archived file: {}", oldFile.getPath());
                updatedFiles.add(oldFile);
                continue;
            }

            DocumentSaveRequest.FilePathVersion newFile = newFilesMap.get(oldFile.getDocName());

            if (newFile == null) {
                // Case 4: File deleted (local)
                Path oldLocalPath = Paths.get(documentStoragePath, oldFile.getPath());
                try {
                    Files.deleteIfExists(oldLocalPath);
                    log.info("🗑️ Deleted local file: {}", oldLocalPath);
                } catch (Exception e) {
                    log.error("FAILED → Delete local file | path={} reason={}",
                            oldLocalPath, e.getMessage(), e);
                    throw new RuntimeException("Failed to delete local file: " + oldLocalPath, e);
                }
                documentDetailsRepository.delete(oldFile);
                log.info("Deleted file record from DB | detailId={}", oldFile.getId());
                continue;
            }

            String newFileName = Paths.get(newFile.getPath()).getFileName().toString();

            // Resolve year per file
            YearMaster newYearMaster = yearMasterRepository.findById(Math.toIntExact(newFile.getYearId()))
                    .orElseThrow(() -> {
                        log.error("YearMaster not found | yearId={}", newFile.getYearId());
                        return new ResourceNotFoundException("YearMaster not found with id " + newFile.getYearId());
                    });

            // Regenerate path
            String newRelativePath = generateNewPath(branch, department, newYearMaster, categoryMaster, newFile.getVersion(), newFileName);

            boolean pathChanged = !oldFile.getPath().equals(newRelativePath);
            boolean versionChanged = !oldFile.getVersion().equals(newFile.getVersion());
            boolean yearChanged = !oldFile.getYearMaster().getId().equals(newFile.getYearId());

            if (pathChanged || versionChanged || yearChanged) {
                // Case 2: Move locally
                Path oldLocalPath = Paths.get(documentStoragePath, oldFile.getPath());
                Path newLocalDir = Paths.get(documentStoragePath, newRelativePath).getParent();

                try {
                    Files.createDirectories(newLocalDir); // ensure dir exists
                    Files.move(oldLocalPath, newLocalDir.resolve(newFileName), StandardCopyOption.REPLACE_EXISTING);
                    log.info("📂 Moved local file: {} → {}", oldLocalPath, newRelativePath);
                } catch (Exception e) {
                    log.error("FAILED → Move local file | from={} to={} reason={}",
                            oldLocalPath, newRelativePath, e.getMessage(), e);
                    throw new RuntimeException("Failed to move local file: " + oldLocalPath + " → " + newRelativePath, e);
                }

                oldFile.setPath(newRelativePath);
                oldFile.setVersion(newFile.getVersion());
                oldFile.setDocName(newFileName);
                oldFile.setYearMaster(newYearMaster);

                log.debug("Updated file metadata | detailId={} newPath={} newVersion={}",
                        oldFile.getId(), newRelativePath, newFile.getVersion());
            } else {
                // Case 1: No change
                log.debug("✅ Unchanged file: {}", oldFile.getPath());
            }

            oldFile.setUpdatedOn(new Timestamp(System.currentTimeMillis()));
            documentDetailsRepository.save(oldFile);
            updatedFiles.add(oldFile);
        }

        // ✅ 2. Process new files (including waiting room files)
        log.debug("Processing {} new file entries", filePaths.size());

        for (DocumentSaveRequest.FilePathVersion newFile : filePaths) {
            String fileName = Paths.get(newFile.getPath()).getFileName().toString();

            boolean existsInDb = updatedFiles.stream()
                    .anyMatch(f -> f.getDocName().equals(fileName) && f.getVersion().equals(newFile.getVersion()));

            if (!existsInDb) {
                // Case 3: New file → only add DB entry (file already moved in moveWaitingRoomFilesToDocumentStorage)
                YearMaster newYearMaster = yearMasterRepository.findById(Math.toIntExact(newFile.getYearId()))
                        .orElseThrow(() -> {
                            log.error("YearMaster not found | yearId={}", newFile.getYearId());
                            return new ResourceNotFoundException("YearMaster not found with id " + newFile.getYearId());
                        });

                DocumentDetails documentDetails = new DocumentDetails();
                documentDetails.setDocName(fileName);
                documentDetails.setPath(newFile.getPath()); // use provided path (already moved to document storage)
                documentDetails.setVersion(newFile.getVersion());
                documentDetails.setDocumentHeader(documentHeader);
                documentDetails.setYearMaster(newYearMaster);
                documentDetails.setPageCounts(newFile.getPageCounts());
                documentDetails.setFileSizeHuman(newFile.getFileSizeHuman());
                documentDetails.setFileSizeBytes(newFile.getFileSizeBytes());
                documentDetails.setMimeType(newFile.getMimeType());
                documentDetails.setFileType(newFile.getFileType());

                // Handle waiting room reference for new files in update
                if (Boolean.TRUE.equals(newFile.getIsWaitingRoomFile()) && newFile.getWaitingRoomId() != null) {
                    WaitingRoom waitingRoom = waitingRoomRepository.findById(newFile.getWaitingRoomId())
                            .orElseThrow(() -> {
                                log.error("Waiting room not found | waitingRoomId={}", newFile.getWaitingRoomId());
                                return new ResourceNotFoundException("Waiting room not found with ID: " + newFile.getWaitingRoomId());
                            });
                    documentDetails.setWaitingRoomId(waitingRoom);
                    log.debug("Associated waiting room file | waitingRoomId={} fileName={}",
                            newFile.getWaitingRoomId(), fileName);
                }

                documentDetails.setCreatedOn(new Timestamp(System.currentTimeMillis()));

                documentDetailsRepository.save(documentDetails);
                updatedFiles.add(documentDetails);

                documentActivityReportService.logAction(
                        documentHeader,
                        documentDetails,
                        ActionTypeForReport.UPLOAD,
                        "SUCCESS",
                        currentUser.getCurrentEmployeeOrThrow(),
                        null, // request not available here
                        Map.of(
                                "version", documentDetails.getVersion(),
                                "fileSize", documentDetails.getFileSizeBytes()
                        )
                );

                log.info("➕ Added new file entry (DB only): {}", newFile.getPath());
            }
        }

        log.info("SUCCESS → Updated file details | headerId={} totalFiles={}",
                documentHeader.getId(), updatedFiles.size());

        return updatedFiles;
    }



    private String generateNewPath(String branch, String department, YearMaster yearMaster, CategoryMaster categoryMaster, String version, String fileName) {
        return String.format("%s/%s/%s/%s/%s/%s",
                branch,
                department,
                yearMaster.getName(),
                categoryMaster.getName().replaceAll("\\s+", "_"),
                version,
                fileName);
    }


    @Override
    public List<DocumentDetailsResponse> findDocumentsByHeaderId(
            Integer headerId, DocApprovalStatus status) {

        log.info("API CALL → Find Documents By Header ID with Status | headerId={} status={}",
                headerId, status);

        List<DocumentDetails> documentDetails = documentDetailsRepository.findByHeaderIdAndStatus(headerId, status);

        log.debug("Found {} document details for header {} with status {}",
                documentDetails.size(), headerId, status);

        return documentDetails.stream()
                .map(details -> {
                    DocumentDetailsResponse response = new DocumentDetailsResponse();
                    response.setId(details.getId());
                    response.setDocName(details.getDocName());
                    response.setPath(details.getPath());
                    response.setVersion(details.getVersion());
                    response.setCreatedOn(details.getCreatedOn());
                    response.setUpdatedOn(details.getUpdatedOn());
                    response.setYear(details.getYearMaster().getName());
                    response.setStatus(details.getStatus().name());
                    response.setApprovedBy(details.getApprovedBy());
                    response.setCreatedBy(details.getCreatedBy());
                    response.setApprovedOn(details.getApprovedOn());
                    response.setRejectionReason(details.getRejectionReason());
                    response.setUpdetedBy(details.getUpdatedBy());
                    return response;
                })
                .collect(Collectors.toList());
    }


    @Override
    public List<DocumentDetailsResponse> findDocumentsByHeaderId(Integer headerId) {
        log.info("API CALL → Find Documents By Header ID | headerId={}", headerId);

        List<DocumentDetails> documentDetails = documentDetailsRepository.findByDocumentHeaderId(headerId);

        log.debug("Found {} document details for header {}", documentDetails.size(), headerId);

        return documentDetails.stream()
                .map(details -> {
                    DocumentDetailsResponse response = new DocumentDetailsResponse();
                    response.setId(details.getId());
                    response.setDocName(details.getDocName());
                    response.setPath(details.getPath());
                    response.setVersion(details.getVersion());
                    response.setCreatedOn(details.getCreatedOn());
                    response.setUpdatedOn(details.getUpdatedOn());
                    response.setYear(details.getYearMaster().getName());
                    response.setStatus(details.getStatus().name());
                    response.setApprovedBy(details.getApprovedBy());
                    response.setCreatedBy(details.getCreatedBy());
                    response.setApprovedOn(details.getApprovedOn());
                    response.setRejectionReason(details.getRejectionReason());
                    return response;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<FileTypeCountDTO> getTop10FileTypesByYear() {
        log.info("API CALL → Get Top 10 File Types By Year");

        List<Object[]> rawResult = documentDetailsRepository.findAllFileTypeCountsByYear();

        log.debug("Raw file type counts retrieved: {} records", rawResult.size());

        Map<Integer, List<FileTypeCountDTO>> groupedByYear = rawResult.stream()
                .map(obj -> new FileTypeCountDTO(
                        ((Number) obj[0]).intValue(),
                        (String) obj[1],
                        ((Number) obj[2]).longValue()
                ))
                .collect(Collectors.groupingBy(FileTypeCountDTO::getYear));

        List<FileTypeCountDTO> result = groupedByYear.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream()
                        .sorted(Comparator.comparingLong(FileTypeCountDTO::getFileCount).reversed())
                        .limit(10)
                )
                .collect(Collectors.toList());

        log.info("SUCCESS → Retrieved top file types by year | totalEntries={}", result.size());

        return result;
    }

    // ===================== COMPARE FILES =====================
    @Override
    public ApiResponse<FileCompareResponse> compareFiles(FileCompareRequest request) {
        log.info("API CALL → Compare Files | file1Id={} file2Id={}",
                request.getFirstFileId(), request.getSecondFileId());

        Optional<DocumentDetails> file1Opt = documentDetailsRepository.findById(request.getFirstFileId());
        Optional<DocumentDetails> file2Opt = documentDetailsRepository.findById(request.getSecondFileId());

        if (file1Opt.isEmpty() || file2Opt.isEmpty()) {
            log.error("One or both files not found | file1Id={} file2Id={}",
                    request.getFirstFileId(), request.getSecondFileId());
            return ResponseUtils.createNotFoundResponse("One or both files not found", HttpStatus.NOT_FOUND.value());
        }

        DocumentDetails file1 = file1Opt.get();
        DocumentDetails file2 = file2Opt.get();

        try {
            Path p1 = resolveWithFallback(file1.getPath());
            Path p2 = resolveWithFallback(file2.getPath());

            if (!Files.exists(p1) || !Files.exists(p2)) {
                log.error("File not found -> p1: {} exists:{} | p2: {} exists:{}",
                        p1, Files.exists(p1), p2, Files.exists(p2));
                return ResponseUtils.createFailureResponse(
                        null, new TypeReference<FileCompareResponse>() {},
                        "One or both file paths are invalid", HttpStatus.BAD_REQUEST.value());
            }

            // ✅ Detect MIME type from DECRYPTED stream (not encrypted path)
            String mime1 = detectMimeFromEncryptedFile(p1, file1.getDocName());
            String mime2 = detectMimeFromEncryptedFile(p2, file2.getDocName());

            log.debug("Detected MIME types -> file1: {} | file2: {}", mime1, mime2);

            String fileType1 = getFileTypeFromName(file1.getDocName());
            String fileType2 = getFileTypeFromName(file2.getDocName());

            log.debug("File1 type (ext): '{}' | File2 type (ext): '{}'", fileType1, fileType2);

            List<FileCompareResponse.DifferenceHighlight> differences = new ArrayList<>();
            FileCompareResponse.ComparisonSummary summary;
            boolean identical;
            double similarity;

            FileCompareResponse.FileContent leftFile;
            FileCompareResponse.FileContent rightFile;
            FileCompareResponse compareResponse;

            // IMAGE branch
            if (isImage(mime1) && isImage(mime2)) {
                log.debug("Comparing image files");

                // ✅ Read images from decrypted streams
                ImageCompareResult imgResult = compareEncryptedImagesAndGenerateDiff(
                        p1, p2, Paths.get(documentStoragePath).normalize());

                identical = imgResult.identical;
                similarity = imgResult.similarityPercent;

                summary = new FileCompareResponse.ComparisonSummary(0, 0, 0, 0, similarity);

                leftFile = new FileCompareResponse.FileContent(
                        file1.getDocName(), nv(file1.getVersion(), "1"),
                        file1.getPath(), Collections.emptyList(), fileType1);
                rightFile = new FileCompareResponse.FileContent(
                        file2.getDocName(), nv(file2.getVersion(), "1"),
                        file2.getPath(), Collections.emptyList(), fileType2);

                FileCompareResponse.FileComparisonResult comparisonResult =
                        new FileCompareResponse.FileComparisonResult(leftFile, rightFile, differences, summary);

                compareResponse = new FileCompareResponse(
                        identical, identical ? "Images are identical" : "Images are different", similarity);
                compareResponse.setComparisonResult(comparisonResult);
                compareResponse.setDifferences(differences);
                compareResponse.setDiffImagePath(imgResult.visualDiffRelativePath);

                log.info("SUCCESS → Image comparison completed | identical={} similarity={}%",
                        identical, similarity);

                return ResponseUtils.createSuccessResponse(compareResponse, new TypeReference<FileCompareResponse>() {});
            }
            // DOCUMENT/PDF branch
            else {
                log.debug("Comparing document files");

                // ✅ Decrypt then extract text
                String content1 = extractTextFromEncryptedFile(p1);
                String content2 = extractTextFromEncryptedFile(p2);

                List<String> lines1 = Arrays.asList(content1.split("\\R"));
                List<String> lines2 = Arrays.asList(content2.split("\\R"));

                identical = content1.equals(content2);
                similarity = calculateSimilarity(content1, content2);
                summary = generateDetailedComparison(lines1, lines2, differences);
                summary.setSimilarityPercentage(similarity);

                leftFile = new FileCompareResponse.FileContent(
                        file1.getDocName(), nv(file1.getVersion(), "1"),
                        file1.getPath(), lines1, fileType1);
                rightFile = new FileCompareResponse.FileContent(
                        file2.getDocName(), nv(file2.getVersion(), "1"),
                        file2.getPath(), lines2, fileType2);

                FileCompareResponse.FileComparisonResult comparisonResult =
                        new FileCompareResponse.FileComparisonResult(leftFile, rightFile, differences, summary);

                compareResponse = new FileCompareResponse(
                        identical, identical ? "Files are identical" : "Files are different", similarity);
                compareResponse.setComparisonResult(comparisonResult);
                compareResponse.setDifferences(differences);

                log.info("SUCCESS → Document comparison completed | identical={} similarity={}%",
                        identical, similarity);

                return ResponseUtils.createSuccessResponse(compareResponse, new TypeReference<FileCompareResponse>() {});
            }

        } catch (Exception e) {
            log.error("ERROR → Compare files | file1Id={} file2Id={} reason={}",
                    request.getFirstFileId(), request.getSecondFileId(), e.getMessage(), e);
            return ResponseUtils.createFailureResponse(
                    null, new TypeReference<FileCompareResponse>() {},
                    "Error comparing files: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    // ✅ NEW: Detect MIME from decrypted stream with filename hint fallback
    private String detectMimeFromEncryptedFile(Path filePath, String docName) {
        try (InputStream fileStream = Files.newInputStream(filePath);
             CipherInputStream decryptedStream = fileEncryptionUtil.decrypt(fileStream)) {
            return tika.detect(decryptedStream, docName);
        } catch (Exception e) {
            log.warn("Could not detect MIME from decrypted stream for {}, falling back to name detection", docName);
            // Fallback: detect from filename only
            return tika.detect(docName);
        }
    }

    // ✅ NEW: Extract text from encrypted file by decrypting first
    private String extractTextFromEncryptedFile(Path filePath) throws Exception {

        log.info("========== PDF DEBUG START ==========");
        log.info("File Path   : {}", filePath);
        log.info("Exists      : {}", Files.exists(filePath));
        log.info("Readable    : {}", Files.isReadable(filePath));
        log.info("File Size   : {} bytes", Files.size(filePath));

        try (InputStream fileStream = Files.newInputStream(filePath);
             CipherInputStream decryptedStream = fileEncryptionUtil.decrypt(fileStream)) {

            byte[] data = decryptedStream.readAllBytes();

            log.info("Decrypted Size : {} bytes", data.length);

            if (data.length == 0) {
                throw new IOException("Decrypted file is empty");
            }

            // Test PDFBox first
            try {
                PDDocument document = PDDocument.load(data);
                log.info("PDFBox SUCCESS - Pages: {}", document.getNumberOfPages());
                document.close();
            } catch (Exception ex) {
                log.error("PDFBox FAILED", ex);
                throw ex;
            }

            // Then test Tika
            Metadata metadata = new Metadata();
            metadata.set(
                    TikaCoreProperties.RESOURCE_NAME_KEY,
                    filePath.getFileName().toString()
            );

            String extractedText =
                    tika.parseToString(new ByteArrayInputStream(data), metadata);

            log.info("Tika SUCCESS - Extracted Characters: {}", extractedText.length());
            log.info("========== PDF DEBUG END ==========");

            return extractedText;

        } catch (Exception e) {

            log.error("TIKA/PDF ERROR", e);

            Throwable root = e;
            while (root.getCause() != null) {
                root = root.getCause();
            }

            log.error("ROOT CAUSE: {}", root.getMessage(), root);

            throw e;
        }
    }
    // ✅ NEW: Compare images by decrypting both first
    private ImageCompareResult compareEncryptedImagesAndGenerateDiff(Path p1, Path p2, Path base) throws Exception {
        log.debug("Starting encrypted image comparison: {} vs {}", p1, p2);

        BufferedImage img1;
        BufferedImage img2;

        try (InputStream s1 = Files.newInputStream(p1);
             CipherInputStream d1 = fileEncryptionUtil.decrypt(s1)) {
            img1 = ImageIO.read(d1);
        }

        try (InputStream s2 = Files.newInputStream(p2);
             CipherInputStream d2 = fileEncryptionUtil.decrypt(s2)) {
            img2 = ImageIO.read(d2);
        }

        if (img1 == null || img2 == null) {
            throw new IOException("Failed to read one or both decrypted images.");
        }

        if (img1.getWidth() != img2.getWidth() || img1.getHeight() != img2.getHeight()) {
            log.debug("Resizing image2 to match image1 dimensions");
            img2 = scale(img2, img1.getWidth(), img1.getHeight());
        }

        double same = 0, total = (double) img1.getWidth() * img1.getHeight();
        BufferedImage diffImg = new BufferedImage(img1.getWidth(), img1.getHeight(), BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < img1.getHeight(); y++) {
            for (int x = 0; x < img1.getWidth(); x++) {
                int rgb1 = img1.getRGB(x, y);
                int rgb2 = img2.getRGB(x, y);
                if (rgb1 == rgb2) {
                    diffImg.setRGB(x, y, rgb1);
                    same++;
                } else {
                    diffImg.setRGB(x, y, Color.RED.getRGB());
                }
            }
        }

        double similarity = (same / total) * 100.0;

        String diffsDirRel = "_diffs";
        Path diffsDir = base.resolve(diffsDirRel).normalize();
        Files.createDirectories(diffsDir);
        String outName = "imgdiff_" + System.currentTimeMillis() + ".png";
        Path out = diffsDir.resolve(outName);
        ImageIO.write(diffImg, "png", out.toFile());

        ImageCompareResult r = new ImageCompareResult();
        r.identical = similarity == 100.0;
        r.similarityPercent = similarity;
        r.visualDiffRelativePath = diffsDirRel + "/" + outName;

        log.debug("Image comparison done | identical={} similarity={}% diffPath={}",
                r.identical, r.similarityPercent, r.visualDiffRelativePath);

        return r;
    }

    private String sanitizeStoredPath(String p) {
        if (p == null) return "";
        String cleaned = p.trim();
        if (cleaned.startsWith("ARCHIVED:")) {
            cleaned = cleaned.substring("ARCHIVED:".length());
        }
        cleaned = cleaned.replace("\\", "/");
        while (cleaned.startsWith("/")) cleaned = cleaned.substring(1);
        return cleaned;
    }

    private boolean isImage(String mime) {
        return mime != null && mime.startsWith("image/");
    }

    private String nv(String v, String d) {
        return (v == null || v.isBlank()) ? d : v;
    }

    private String getFileTypeFromName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return "unknown";
        }

        String cleanFileName = fileName.trim();
        if (cleanFileName.contains("/")) {
            cleanFileName = cleanFileName.substring(cleanFileName.lastIndexOf("/") + 1);
        }
        if (cleanFileName.contains("\\")) {
            cleanFileName = cleanFileName.substring(cleanFileName.lastIndexOf("\\") + 1);
        }
        if (!cleanFileName.contains(".") || cleanFileName.endsWith(".")) {
            return "unknown";
        }

        String extension = cleanFileName.substring(cleanFileName.lastIndexOf(".") + 1).toLowerCase();
        if (extension.contains("?")) {
            extension = extension.substring(0, extension.indexOf("?"));
        }
        if (extension.contains("#")) {
            extension = extension.substring(0, extension.indexOf("#"));
        }
        return extension.isEmpty() || extension.length() > 10 ? "unknown" : extension;
    }

    private FileCompareResponse.ComparisonSummary generateDetailedComparison(
            List<String> lines1,
            List<String> lines2,
            List<FileCompareResponse.DifferenceHighlight> differences) {

        int totalLinesAdded = 0;
        int totalLinesDeleted = 0;
        int totalLinesModified = 0;
        int maxLines = Math.max(lines1.size(), lines2.size());

        for (int i = 0; i < maxLines; i++) {
            String line1 = i < lines1.size() ? lines1.get(i) : null;
            String line2 = i < lines2.size() ? lines2.get(i) : null;

            if (line1 == null && line2 != null) {
                differences.add(new FileCompareResponse.DifferenceHighlight(-1, i + 1, "ADDED", "", line2, "#d4f6d4"));
                totalLinesAdded++;
            } else if (line1 != null && line2 == null) {
                differences.add(new FileCompareResponse.DifferenceHighlight(i + 1, -1, "DELETED", line1, "", "#f6d4d4"));
                totalLinesDeleted++;
            } else if (line1 != null && line2 != null && !line1.equals(line2)) {
                differences.add(new FileCompareResponse.DifferenceHighlight(i + 1, i + 1, "MODIFIED", line1, line2, "#fff2d4"));
                totalLinesModified++;
            }
        }

        return new FileCompareResponse.ComparisonSummary(
                totalLinesAdded, totalLinesDeleted, totalLinesModified, differences.size(), 0.0);
    }

    private double calculateSimilarity(String s1, String s2) {
        if (s1.isEmpty() && s2.isEmpty()) return 100.0;
        int distance = levenshteinDistance(s1, s2);
        int maxLength = Math.max(s1.length(), s2.length());
        if (maxLength == 0) return 100.0;
        return ((double) (maxLength - distance) / maxLength) * 100.0;
    }

    private int levenshteinDistance(String s1, String s2) {
        int m = s1.length(), n = s2.length();
        int[][] dp = new int[m + 1][n + 1];
        for (int i = 0; i <= m; i++) {
            for (int j = 0; j <= n; j++) {
                if (i == 0) dp[i][j] = j;
                else if (j == 0) dp[i][j] = i;
                else if (s1.charAt(i - 1) == s2.charAt(j - 1)) dp[i][j] = dp[i - 1][j - 1];
                else dp[i][j] = 1 + Math.min(dp[i][j - 1], Math.min(dp[i - 1][j], dp[i - 1][j - 1]));
            }
        }
        return dp[m][n];
    }

    private static class ImageCompareResult {
        boolean identical;
        double similarityPercent;
        String visualDiffRelativePath;
    }

    private ImageCompareResult compareImagesAndGenerateDiff(Path p1, Path p2, Path base) throws IOException {
        log.debug("Starting image comparison: {} vs {}", p1, p2);

        BufferedImage img1 = ImageIO.read(p1.toFile());
        BufferedImage img2 = ImageIO.read(p2.toFile());

        if (img1 == null || img2 == null) {
            throw new IOException("Failed to read one of the images.");
        }

        if (img1.getWidth() != img2.getWidth() || img1.getHeight() != img2.getHeight()) {
            log.debug("Resizing image2 to match image1 dimensions");
            img2 = scale(img2, img1.getWidth(), img1.getHeight());
        }

        double same = 0, total = (double) img1.getWidth() * img1.getHeight();
        BufferedImage diffImg = new BufferedImage(img1.getWidth(), img1.getHeight(), BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < img1.getHeight(); y++) {
            for (int x = 0; x < img1.getWidth(); x++) {
                int rgb1 = img1.getRGB(x, y);
                int rgb2 = img2.getRGB(x, y);
                if (rgb1 == rgb2) {
                    diffImg.setRGB(x, y, rgb1);
                    same++;
                } else {
                    diffImg.setRGB(x, y, Color.RED.getRGB());
                }
            }
        }

        double similarity = (same / total) * 100.0;

        String diffsDirRel = "_diffs";
        Path diffsDir = base.resolve(diffsDirRel).normalize();
        Files.createDirectories(diffsDir);
        String outName = "imgdiff_" + System.currentTimeMillis() + ".png";
        Path out = diffsDir.resolve(outName);

        ImageIO.write(diffImg, "png", out.toFile());

        ImageCompareResult r = new ImageCompareResult();
        r.identical = similarity == 100.0;
        r.similarityPercent = similarity;
        r.visualDiffRelativePath = diffsDirRel + "/" + outName;

        log.debug("Image comparison completed | identical={} similarity={}% diffPath={}",
                r.identical, r.similarityPercent, r.visualDiffRelativePath);

        return r;
    }

    private BufferedImage scale(BufferedImage src, int w, int h) {
        Image tmp = src.getScaledInstance(w, h, Image.SCALE_SMOOTH);
        BufferedImage resized = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resized.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();
        return resized;
    }

    private Path resolveWithFallback(String storedPath) {
        String cleaned = sanitizeStoredPath(storedPath);

        Path baseStorage = Paths.get(documentStoragePath).normalize();
        Path baseArchive = Paths.get(documentArchivePath).normalize();

        Path primary = baseStorage.resolve(cleaned).normalize();
        if (Files.exists(primary)) {
            log.debug("Using storage path: {}", primary);
            return primary;
        }

        Path fallback = baseArchive.resolve(cleaned).normalize();
        if (Files.exists(fallback)) {
            log.debug("Using archive path: {}", fallback);
            return fallback;
        }

        log.warn("File not found in both storage and archive: {}", cleaned);
        return primary;
    }





    @Override
    @Transactional
    public List<Integer> saveFileDetailsWithWaitingRoom(DocumentHeader header,
                                                        List<DocumentSaveRequest.FilePathVersion> filePaths,
                                                        String userEmail) {
        log.info("API CALL → Save File Details With Waiting Room | headerId={} fileCount={} userEmail={}",
                header.getId(), filePaths.size(), userEmail);

        List<Integer> waitingRoomIds = new ArrayList<>();
        Timestamp now = new Timestamp(System.currentTimeMillis());

        for (DocumentSaveRequest.FilePathVersion fp : filePaths) {
            try {
                DocumentDetails detail = new DocumentDetails();
                detail.setDocumentHeader(header);
                detail.setPath(fp.getPath());
                detail.setVersion(fp.getVersion());
                detail.setStatus(DocApprovalStatus.PENDING);

                // Extract file name from path
                String fileName = fp.getPath();
                if (fileName.contains("/")) {
                    fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
                }
                detail.setDocName(fileName);

                // ✅ Handle YearMaster lookup
                if (fp.getYearId() != null) {
                    YearMaster year = yearMasterRepository.findById(fp.getYearId().intValue())
                            .orElseThrow(() -> {
                                log.error("Year not found | yearId={}", fp.getYearId());
                                return new ResourceNotFoundException("Year not found with ID: " + fp.getYearId());
                            });
                    detail.setYearMaster(year);
                    log.debug("Associated year | year={} for file={}", year.getName(), fileName);
                }

                // ✅ File metadata
                detail.setFileType(fp.getFileType());
                detail.setMimeType(fp.getMimeType());
                detail.setFileSizeBytes(fp.getFileSizeBytes());
                detail.setFileSizeHuman(fp.getFileSizeHuman());
                detail.setPageCounts(fp.getPageCounts());

                // ✅ Handle Waiting Room reference properly
                if (Boolean.TRUE.equals(fp.getIsWaitingRoomFile()) && fp.getWaitingRoomId() != null) {
                    WaitingRoom waitingRoom = waitingRoomRepository.findById(fp.getWaitingRoomId())
                            .orElseThrow(() -> {
                                log.error("Waiting room not found | waitingRoomId={}", fp.getWaitingRoomId());
                                return new ResourceNotFoundException("Waiting room not found with ID: " + fp.getWaitingRoomId());
                            });
                    detail.setWaitingRoomId(waitingRoom);  // ✅ Assign entity, not ID
                    waitingRoomIds.add(fp.getWaitingRoomId());
                    log.debug("Associated waiting room | waitingRoomId={} for file={}",
                            fp.getWaitingRoomId(), fileName);
                }

                // ✅ Audit fields
                detail.setCreatedOn(now);
                detail.setCreatedBy(userEmail);
                detail.setUpdatedOn(now);
                detail.setUpdatedBy(userEmail);
                detail.setArchive(false);
                detail.setRestored(false);

                documentDetailsRepository.save(detail);
                log.debug("Saved file detail | fileName={} detailId={}", fileName, detail.getId());

                documentActivityReportService.logAction(
                        header,
                        detail,
                        ActionTypeForReport.UPLOAD,
                        "SUCCESS",
                        currentUser.getCurrentEmployeeOrThrow(), // actor
                        null, // HttpServletRequest not available here
                        Map.of(
                                "source", Boolean.TRUE.equals(fp.getIsWaitingRoomFile()) ? "WAITING_ROOM" : "DIRECT",
                                "version", detail.getVersion(),
                                "fileSize", detail.getFileSizeBytes()
                        )
                );
            } catch (Exception e) {
                log.error("FAILED → Save file detail with waiting room | path={} reason={}",
                        fp.getPath(), e.getMessage(), e);
                throw e;
            }
        }

        log.info("SUCCESS → Saved {} file details with waiting room references", filePaths.size());
        return waitingRoomIds;
    }




    @Override
    @Transactional
    public DocumentDetails updateDeleteStatus(Integer detailId, Boolean isDeleted, HttpServletRequest request) {
        log.info("API CALL → Update Delete Status | detailId={} isDeleted={}", detailId, isDeleted);

        Employee empObj = currentUser.getCurrentEmployeeOrThrow();

        // Fetch the document detail
        DocumentDetails documentDetail = documentDetailsRepository.findById(detailId)
                .orElseThrow(() -> {
                    log.error("Document detail not found | detailId={}", detailId);
                    return new ResourceNotFoundException("Document detail not found with id: " + detailId);
                });

        // Check if document is approved
        if (documentDetail.getStatus() != DocApprovalStatus.APPROVED) {
            log.warn("Only approved documents can be deleted/restored | detailId={} status={}",
                    detailId, documentDetail.getStatus());
            throw new IllegalArgumentException("Only approved documents can be deleted/restored");
        }

        // Store old value for audit
        Boolean oldStatus = documentDetail.getIsDeleted();

        try {
            log.debug("Updating delete status | detailId={} oldStatus={} newStatus={}",
                    detailId, oldStatus, isDeleted);

            // Update delete status
            documentDetail.setIsDeleted(isDeleted);
            documentDetail.setUpdatedOn(new Timestamp(System.currentTimeMillis()));
            documentDetail.setUpdatedBy(empObj.getEmail());

            // Save the updated document
            DocumentDetails updatedDetail = documentDetailsRepository.save(documentDetail);

            // Prepare audit details
            Map<String, Object> detailsJson = new HashMap<>();
            detailsJson.put("documentId", detailId);
            detailsJson.put("documentName", documentDetail.getDocName());
            detailsJson.put("oldIsDeleted", oldStatus);
            detailsJson.put("newIsDeleted", isDeleted);
            detailsJson.put("action", isDeleted ? "Trash" : "Restore");

            // Log audit trail
            auditLogUtil.logDocumentAction(
                    empObj,
                    "Document",
                    isDeleted ? "Trash" : "Restore",
                    "Success",
                    documentDetail.getDocumentHeader().getId(),
                    null,
                    detailsJson,
                    request
            );

            log.info("SUCCESS → Updated delete status | detailId={} action={}",
                    detailId, isDeleted ? "Trash" : "Restore");

            documentActivityReportService.logAction(
                    documentDetail.getDocumentHeader(),
                    documentDetail,
                    isDeleted ? ActionTypeForReport.TRASH : ActionTypeForReport.UNTRASH,
                    "SUCCESS",
                    empObj,
                    request,
                    Map.of(
                            "oldIsDeleted", oldStatus,
                            "newIsDeleted", isDeleted
                    )
            );


            return updatedDetail;

        } catch (Exception ex) {
            log.error("FAILED → Update delete status | detailId={} reason={}",
                    detailId, ex.getMessage(), ex);

            // Log failure in audit
            Map<String, Object> detailsJson = new HashMap<>();
            detailsJson.put("documentId", detailId);
            detailsJson.put("action", isDeleted ? "Trash" : "Restore");
            detailsJson.put("errorMessage", ex.getMessage());

            auditLogUtil.logDocumentAction(
                    empObj,
                    "Document",
                    isDeleted ? "Trash" : "Restore",
                    "Failed",
                    documentDetail.getDocumentHeader().getId(),
                    null,
                    detailsJson,
                    request
            );

            throw new RuntimeException("Failed to update delete status: " + ex.getMessage(), ex);
        }
    }





//counts

    @Override
    public Long countTotalDocByBranchId(Integer branch) {
        log.info("API CALL → Count Total Documents By Branch | branchId={}", branch);
        Long count = documentDetailsRepository.countTotalDetailsByBranch(branch);
        log.info("SUCCESS → Counted {} total documents for branch {}", count, branch);
        return count;
    }


    @Override
    public Long countApprovedByBranchId(Integer branch) {
        log.info("API CALL → Count Approved Documents By Branch | branchId={}", branch);
        Long count = documentDetailsRepository.countApprovedDetailsByBranch(
                branch,
                DocApprovalStatus.APPROVED
        );
        log.info("SUCCESS → Counted {} approved documents for branch {}", count, branch);
        return count;
    }

    @Override
    public Long countPendingDocumentsByBranchId(Integer branch) {
        log.info("API CALL → Count Pending Documents By Branch | branchId={}", branch);
        Long count = documentDetailsRepository.countApprovedDetailsByBranch(
                branch,
                DocApprovalStatus.PENDING
        );
        log.info("SUCCESS → Counted {} pending documents for branch {}", count, branch);
        return count;
    }


    @Override
    public Long countRejectedByBranchId(Integer branch) {
        log.info("API CALL → Count Rejected Documents By Branch | branchId={}", branch);
        Long count = documentDetailsRepository.countApprovedDetailsByBranch(
                branch,
                DocApprovalStatus.REJECTED
        );
        log.info("SUCCESS → Counted {} rejected documents for branch {}", count, branch);
        return count;
    }



    @Override
    public Long countTotalDocByDepartmentId(Integer departmentId) {
        log.info("API CALL → Count Total Documents By Department | departmentId={}", departmentId);
        Long count = documentDetailsRepository.countTotalDetailsByDepartment(departmentId);
        log.info("SUCCESS → Counted {} total documents for department {}", count, departmentId);
        return count;
    }

    @Override
    public Long countPendingDocumentsByDepartmentId(Integer departmentId) {
        log.info("API CALL → Count Pending Documents By Department | departmentId={}", departmentId);
        Long count = documentDetailsRepository.countApprovedDetailsByDepartment(
                departmentId,
                DocApprovalStatus.PENDING
        );
        log.info("SUCCESS → Counted {} pending documents for department {}", count, departmentId);
        return count;
    }

    @Override
    public Long countApprovedDetailsByDepartmentId(Integer departmentId) {
        log.info("API CALL → Count Approved Documents By Department | departmentId={}", departmentId);
        Long count = documentDetailsRepository.countApprovedDetailsByDepartment(
                departmentId,
                DocApprovalStatus.APPROVED
        );
        log.info("SUCCESS → Counted {} approved documents for department {}", count, departmentId);
        return count;
    }

    @Override
    public Long countRejectedByDepartmentId(Integer departmentId) {
        log.info("API CALL → Count Rejected Documents By Department | departmentId={}", departmentId);
        Long count = documentDetailsRepository.countApprovedDetailsByDepartment(
                departmentId,
                DocApprovalStatus.REJECTED
        );
        log.info("SUCCESS → Counted {} rejected documents for department {}", count, departmentId);
        return count;
    }


    @Override
    public Long countApprovedDetails(DocApprovalStatus status) {
        log.info("API CALL → Count Approved Details | status={}", status);
        Long count = documentDetailsRepository.countApprovedDetails(status);
        log.info("SUCCESS → Counted {} documents with status {}", count, status);
        return count;
    }



    //Generate next version number
    @Override
    public String generateNextVersion(Integer headerId, Integer yearId) {
        log.info("Generating next version for headerId: {}, yearId: {}", headerId, yearId);

        // Get all versions for this document and year
        List<DocumentDetails> existingDocs = documentDetailsRepository
                .findByDocumentHeaderIdAndYearMasterIdOrderByVersionDesc(headerId, yearId);

        // If no existing documents, start with 1.0.0
        if (existingDocs.isEmpty()) {
            log.debug("No existing versions found, starting with 1.0.0");
            return "1.0.0";
        }

        String latestVersion = existingDocs.get(0).getVersion();

        // Validate latest version
        if (!isValidSemanticVersion(latestVersion)) {
            log.warn("Latest version {} is not semantic, defaulting to 1.0.0", latestVersion);
            return "1.0.0";
        }

        // Parse latest version and increment patch
        int[] parts = parseVersion(latestVersion);
        int major = parts[0];
        int minor = parts[1];
        int patch = parts[2] + 1;

        // If patch exceeds 999, increment minor and reset patch
        if (patch > 999) {
            patch = 0;
            minor++;
        }

        // If minor exceeds 999, increment major and reset minor
        if (minor > 999) {
            minor = 0;
            major++;
        }

        String nextVersion = formatVersion(major, minor, patch);
        log.info("Generated next version: {} (from {})", nextVersion, latestVersion);

        return nextVersion;
    }

    @Override
    public String getNextVersionWithChangeType(Integer headerId, Integer yearId, String changeType) {
        String currentVersion = generateNextVersion(headerId, yearId);

        // If no version exists, return 1.0.0
        if ("1.0.0".equals(currentVersion)) {
            return currentVersion;
        }

        // Get the actual current version
        List<DocumentDetails> existingDocs = documentDetailsRepository
                .findByDocumentHeaderIdAndYearMasterIdOrderByVersionDesc(headerId, yearId);

        if (existingDocs.isEmpty()) {
            return "1.0.0";
        }

        String latestVersion = existingDocs.get(0).getVersion();
        int[] parts = parseVersion(latestVersion);

        switch (changeType.toLowerCase()) {
            case "major":
                parts[0]++;
                parts[1] = 0;
                parts[2] = 0;
                break;
            case "minor":
                parts[1]++;
                parts[2] = 0;
                break;
            case "patch":
            default:
                parts[2]++;
                break;
        }

        // Handle overflow
        if (parts[2] > 999) {
            parts[2] = 0;
            parts[1]++;
        }
        if (parts[1] > 999) {
            parts[1] = 0;
            parts[0]++;
        }

        return formatVersion(parts[0], parts[1], parts[2]);
    }
    //Get version history with metadata
    @Override
    public Map<String, Object> getVersionHistory(Integer headerId, Integer yearId) {
        log.info("Getting version history for headerId: {}, yearId: {}", headerId, yearId);

        List<DocumentDetails> existingDocs = documentDetailsRepository
                .findByDocumentHeaderIdAndYearMasterIdOrderByVersionDesc(headerId, yearId);

        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> history = new ArrayList<>();

        for (DocumentDetails doc : existingDocs) {
            Map<String, Object> versionInfo = new HashMap<>();
            versionInfo.put("version", doc.getVersion());
            versionInfo.put("createdOn", doc.getCreatedOn());
            versionInfo.put("createdBy", doc.getCreatedBy());
            versionInfo.put("status", doc.getStatus() != null ? doc.getStatus().name() : null);
            versionInfo.put("fileName", doc.getDocName());
            versionInfo.put("fileSize", doc.getFileSizeHuman());
            versionInfo.put("approvedOn", doc.getApprovedOn());
            versionInfo.put("approvedBy", doc.getApprovedBy());
            history.add(versionInfo);
        }

        response.put("history", history);
        response.put("totalVersions", history.size());
        response.put("nextVersion", generateNextVersion(headerId, yearId));

        if (!history.isEmpty()) {
            response.put("latestVersion", history.get(0).get("version"));
        }

        log.debug("Version history retrieved: {} versions", history.size());
        return response;
    }


}