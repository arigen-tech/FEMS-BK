package com.dmsBackend.service.Impl;


import com.dmsBackend.entity.*;
import com.dmsBackend.repository.*;
import com.dmsBackend.response.ArchiveRestoreDTO;
import com.dmsBackend.service.ArchiveRestoreService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@Slf4j
public class ArchiveRestoreServiceImpl implements ArchiveRestoreService {

    @Value("${document.storage.path}")
    private String baseStoragePath;

    private final DocumentHeaderRepository documentHeaderRepository;
    private final DocumentDetailsRepository documentDetailsRepository;
    private final BranchMasterRepository branchRepository;
    private final DepartmentMasterRepository departmentRepository;
    private final YearMasterRepository yearMasterRepository;
    private final CategoryMasterRepository categoryMasterRepository;

    public ArchiveRestoreServiceImpl(
            DocumentHeaderRepository documentHeaderRepository,
            DocumentDetailsRepository documentDetailsRepository,
            BranchMasterRepository branchRepository,
            DepartmentMasterRepository departmentRepository,
            YearMasterRepository yearMasterRepository,
            CategoryMasterRepository categoryMasterRepository) {
        this.documentHeaderRepository = documentHeaderRepository;
        this.documentDetailsRepository = documentDetailsRepository;
        this.branchRepository = branchRepository;
        this.departmentRepository = departmentRepository;
        this.yearMasterRepository = yearMasterRepository;
        this.categoryMasterRepository = categoryMasterRepository;
    }

    @Override
    public void restoreArchive(ArchiveRestoreDTO restoreDTO, HttpServletResponse response) throws IOException {
        validateUserAccess(restoreDTO);

        String tempDir = Files.createTempDirectory("archive_restore_").toString();

        try {
            extractZip(restoreDTO.getFile(), tempDir);
            processExtractedFiles(tempDir, baseStoragePath, restoreDTO);
            log.info("Archive restoration completed successfully for userRole: {}", restoreDTO.getUserRole());
        } catch (Exception e) {
            log.error("Error restoring archive: ", e);
            throw new IOException("Failed to restore archive: " + e.getMessage());
        } finally {
            deleteDirectory(new File(tempDir));
        }
    }

    private void validateUserAccess(ArchiveRestoreDTO restoreDTO) {
        if (!"ADMIN".equals(restoreDTO.getUserRole())) {
            if (restoreDTO.getDepartmentId() != null) {
                DepartmentMaster department = departmentRepository.findById(restoreDTO.getDepartmentId())
                        .orElseThrow(() -> new RuntimeException("Department not found"));
                if (!department.getBranch().getId().equals(restoreDTO.getBranchId())) {
                    throw new RuntimeException("Department does not belong to the specified branch");
                }
            }
        }
    }

    private void extractZip(MultipartFile zipFile, String destDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(zipFile.getInputStream())) {
            ZipEntry zipEntry;
            while ((zipEntry = zis.getNextEntry()) != null) {
                Path newPath = validateAndCreatePath(destDir, zipEntry);

                if (zipEntry.isDirectory()) {
                    Files.createDirectories(newPath);
                } else {
                    Files.createDirectories(newPath.getParent());
                    Files.copy(zis, newPath, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }
    }

    private Path validateAndCreatePath(String destDir, ZipEntry zipEntry) throws IOException {
        Path destPath = Paths.get(destDir, zipEntry.getName());
        String destDirCanonical = new File(destDir).getCanonicalPath();
        String destCanonical = destPath.toFile().getCanonicalPath();

        if (!destCanonical.startsWith(destDirCanonical)) {
            throw new IOException("Entry is outside of target directory: " + zipEntry.getName());
        }
        return destPath;
    }

    private void processExtractedFiles(String extractPath, String finalPath, ArchiveRestoreDTO restoreDTO) throws IOException {
        Files.walk(Paths.get(extractPath))
                .filter(Files::isRegularFile)
                .forEach(file -> {
                    try {
                        if (canProcessFile(file, restoreDTO)) {
                            processFile(file, extractPath, finalPath);
                        }
                    } catch (Exception e) {
                        log.error("Error processing file {}: ", file, e);
                    }
                });
    }

    private boolean canProcessFile(Path file, ArchiveRestoreDTO restoreDTO) {
        if ("ADMIN".equals(restoreDTO.getUserRole())) {
            return true;
        }

        String[] pathComponents = file.toString().split(Pattern.quote(File.separator));
        if (pathComponents.length < 6) return false;

        String branchName = pathComponents[pathComponents.length - 6];
        String deptName = pathComponents[pathComponents.length - 5];

        BranchMaster branch = branchRepository.findById(restoreDTO.getBranchId())
                .orElseThrow(() -> new RuntimeException("Branch not found"));

        if (!sanitizePath(branch.getName()).equals(branchName)) {
            return false;
        }

        if (restoreDTO.getDepartmentId() != null) {
            DepartmentMaster department = departmentRepository.findById(restoreDTO.getDepartmentId())
                    .orElseThrow(() -> new RuntimeException("Department not found"));
            return sanitizePath(department.getName()).equals(deptName);
        }

        return true;
    }

    private void processFile(Path file, String extractPath, String finalPath) throws IOException {
        Path relativePath = Paths.get(extractPath).relativize(file);
        String[] pathComponents = relativePath.toString().split(Pattern.quote(File.separator));

        if (pathComponents.length >= 6) {
            String branchName = pathComponents[0];
            String deptName = pathComponents[1];
            String yearName = pathComponents[2];
            String categoryName = pathComponents[3];
            String version = pathComponents[4];
            String fileName = pathComponents[5];

            Path destinationPath = Paths.get(finalPath, relativePath.toString());
            Files.createDirectories(destinationPath.getParent());
            Files.move(file, destinationPath, StandardCopyOption.REPLACE_EXISTING);

            updateDatabaseRecords(branchName, deptName, yearName, categoryName,
                    version, fileName, destinationPath.toString());
        }
    }

    private String sanitizePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return "Unknown";
        }
        return path.trim()
                .replaceAll("[^a-zA-Z0-9.\\-_]", "_")
                .replaceAll("\\s+", "_");
    }
    private void updateDatabaseRecords(String branchName, String deptName, String yearName,
                                       String categoryName, String version, String fileName,
                                       String filePath) {
        try {
            // Fetch BranchMaster by name
            BranchMaster branch = branchRepository.findByNameIgnoreCase(branchName)
                    .orElseThrow(() -> new RuntimeException("Branch not found: " + branchName));

            // Fetch DepartmentMaster by name and associated branch
            DepartmentMaster department = departmentRepository.findByNameAndBranch(deptName, branch)
                    .orElseThrow(() -> new RuntimeException("Department not found: " + deptName));

            // Fetch YearMaster by name
            YearMaster year = yearMasterRepository.findByName(yearName)
                    .orElseThrow(() -> new RuntimeException("Year not found: " + yearName));

            // Fetch CategoryMaster by name
            CategoryMaster category = categoryMasterRepository.findByName(categoryName)
                    .orElseThrow(() -> new RuntimeException("Category not found: " + categoryName));

            // Create and save DocumentHeader
            DocumentHeader documentHeader = new DocumentHeader();
//            documentHeader.setYearMaster(year);
            documentHeader.setCategoryMaster(category);
            documentHeader.setCreatedOn(Timestamp.valueOf(LocalDateTime.now())); // Ensure timestamps are set
            documentHeader = documentHeaderRepository.save(documentHeader);

            // Create and save DocumentDetails
            DocumentDetails documentDetails = new DocumentDetails();
            documentDetails.setDocumentHeader(documentHeader);
            documentDetails.setPath(filePath);
            documentDetails.setDocName(fileName);
            documentDetails.setVersion(version);
            documentDetails.setCreatedOn(Timestamp.valueOf(LocalDateTime.now())); // Set creation timestamp
            documentDetailsRepository.save(documentDetails);

            log.info("Database records updated successfully for file: {}", fileName);
        } catch (RuntimeException e) {
            log.error("Validation error: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error updating database records for file {}: ", fileName, e);
        }
    }

//    private String sanitizePath(String path) {
//        if (path == null || path.trim().isEmpty()) {
//            return "Unknown";
//        }
//        return path.trim()
//                .replaceAll("[^a-zA-Z0-9.\\-_]", "_")
//                .replaceAll("\\s+", "_");
//    }

    private void deleteDirectory(File dir) {
        try {
            Files.walk(dir.toPath())
                    .sorted((p1, p2) -> -p1.compareTo(p2))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            log.error("Error deleting path {}: ", path, e);
                        }
                    });
        } catch (IOException e) {
            log.error("Error cleaning up temporary directory: ", e);
        }
    }
}
