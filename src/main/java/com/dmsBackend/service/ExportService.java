package com.dmsBackend.service;

import com.dmsBackend.config.ExportConfiguration;
import com.opencsv.CSVWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.*;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportService {

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private final ExportConfiguration exportConfig;

    private final Map<String, ExportStatus> ongoingExports = new ConcurrentHashMap<>();
    private final List<ExportHistory> exportHistory = Collections.synchronizedList(new ArrayList<>());

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter DATE_RANGE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final String DB_MYSQL = "MySQL";
    private static final String DB_POSTGRESQL = "PostgreSQL";

    public File exportDatabaseToCSV(LocalDate fromDate, LocalDate toDate, String exportId) throws Exception {
        validateExportNotInProgress(exportId, "database");
        validateDuplicateExport(fromDate, toDate, "database", exportId);

        try {
            ongoingExports.put(exportId, new ExportStatus("database", fromDate, toDate));

            String dateRangeSuffix = getDateRangeSuffix(fromDate, toDate);
            String exportDir = exportConfig.getTempDirectory() + "/database_" + dateRangeSuffix;
            Files.createDirectories(Paths.get(exportDir));

            DatabaseInfo dbInfo = getCurrentDatabaseInfo();
            List<String> tableNames = getTablesFromCurrentSchema(dbInfo);

            log.info("[{}] Starting database export - DB: {} ({}), Tables: {}",
                    exportId, dbInfo.getDatabaseName(), dbInfo.getDatabaseType(), tableNames.size());
            log.info("[{}] Date Range Applied: {} to {}", exportId, fromDate, toDate);

            int tablesExported = 0;
            int totalRowsExported = 0;

            for (String tableName : tableNames) {
                ExportResult result = exportTableToCSV(tableName, exportDir, fromDate, toDate, exportId, dbInfo);
                if (result.isSuccess()) {
                    tablesExported++;
                    totalRowsExported += result.getRowCount();
                }
            }

            createMetadataFile(exportDir, tableNames, tablesExported, totalRowsExported, fromDate, toDate, dbInfo);

            String zipFileName = exportConfig.getTempDirectory() + "/DMS_Database_Export_" + dateRangeSuffix + ".zip";
            File zipFile = createZipFile(exportDir, zipFileName);

            addToExportHistory("database", fromDate, toDate, exportId, zipFile.getName(), zipFile.length());

            log.info("[{}] Database export completed - Tables: {}/{}, Rows: {}, File: {}",
                    exportId, tablesExported, tableNames.size(), totalRowsExported, zipFile.getName());

            return zipFile;

        } finally {
            ongoingExports.remove(exportId);
        }
    }

    public File exportFilesToZip(LocalDate fromDate, LocalDate toDate, String exportId) throws Exception {
        validateExportNotInProgress(exportId, "files");
        validateDuplicateExport(fromDate, toDate, "files", exportId);

        try {
            ongoingExports.put(exportId, new ExportStatus("files", fromDate, toDate));

            String dateRangeSuffix = getDateRangeSuffix(fromDate, toDate);
            String zipFileName = exportConfig.getTempDirectory() + "/DMS_Files_Export_" + dateRangeSuffix + ".zip";

            log.info("[{}] Starting FILES EXPORT - Date Range: {} to {}", exportId, fromDate, toDate);

            // Log all storage paths for debugging
            log.info("[{}] Storage Paths:", exportId);
            log.info("[{}] - Documents: {}", exportId, exportConfig.getDocumentStoragePath());
            log.info("[{}] - Waiting Room: {}", exportId, exportConfig.getWaitingRoomStoragePath());
            log.info("[{}] - Profiles: {}", exportId, exportConfig.getProfileStoragePath());
            log.info("[{}] - Archive: {}", exportId, exportConfig.getDocumentArchivePath());

            int totalFilesExported = 0;
            try (FileOutputStream fos = new FileOutputStream(zipFileName);
                 ZipOutputStream zos = new ZipOutputStream(fos)) {

                // Add path mapping file FIRST
                addPathMappingToZip(zos, fromDate, toDate);

                // Export ALL storage locations with proper structure - FIXED: Include ALL files when no date range
                totalFilesExported += exportDirectoryToZip(zos, "documents/",
                        exportConfig.getDocumentStoragePath(), fromDate, toDate, exportId);

                totalFilesExported += exportDirectoryToZip(zos, "waiting_room/",
                        exportConfig.getWaitingRoomStoragePath(), fromDate, toDate, exportId);

                totalFilesExported += exportDirectoryToZip(zos, "profiles/",
                        exportConfig.getProfileStoragePath(), fromDate, toDate, exportId);

                totalFilesExported += exportDirectoryToZip(zos, "archive/",
                        exportConfig.getDocumentArchivePath(), fromDate, toDate, exportId);

                // Add summary file
                addExportSummaryToZip(zos, totalFilesExported, fromDate, toDate);

            }

            File zipFile = new File(zipFileName);
            addToExportHistory("files", fromDate, toDate, exportId, zipFile.getName(), zipFile.length());

            log.info("[{}] ✅ FILES EXPORT COMPLETED - Total Files: {}, File: {}",
                    exportId, totalFilesExported, zipFileName);

            return zipFile;

        } finally {
            ongoingExports.remove(exportId);
        }
    }

    public File exportCompleteSystem(LocalDate fromDate, LocalDate toDate, String exportId) throws Exception {
        validateExportNotInProgress(exportId, "complete");
        validateDuplicateExport(fromDate, toDate, "complete", exportId);

        try {
            ongoingExports.put(exportId, new ExportStatus("complete", fromDate, toDate));

            String dateRangeSuffix = getDateRangeSuffix(fromDate, toDate);
            String exportDir = exportConfig.getTempDirectory() + "/DMS_Full_System_Backup_" + dateRangeSuffix;
            Files.createDirectories(Paths.get(exportDir));

            DatabaseInfo dbInfo = getCurrentDatabaseInfo();
            log.info("[{}] Starting COMPLETE SYSTEM EXPORT - DB: {}", exportId, dbInfo.getDatabaseName());
            log.info("[{}] Date Range Applied: {} to {}", exportId, fromDate, toDate);

            // Step 1: Export database
            log.info("[{}] Exporting database...", exportId);
            String dbExportDir = exportDir + "/database_export";
            Files.createDirectories(Paths.get(dbExportDir));
            exportDatabaseToDirectory(dbExportDir, fromDate, toDate, exportId + "_db", dbInfo);

            // Step 2: Export files
            log.info("[{}] Exporting files...", exportId);
            String filesExportDir = exportDir + "/files_export";
            Files.createDirectories(Paths.get(filesExportDir));
            exportFilesToDirectory(filesExportDir, fromDate, toDate, exportId + "_files");

            // Step 3: Create comprehensive metadata
            log.info("[{}] Creating metadata...", exportId);
            createCompleteMetadataFile(exportDir, fromDate, toDate, dbInfo);

            // Step 4: Create final ZIP
            log.info("[{}] Creating final ZIP...", exportId);
            String finalZip = exportConfig.getTempDirectory() + "/DMS_Full_System_Backup_" + dateRangeSuffix + ".zip";
            File finalZipFile = createZipFile(exportDir, finalZip);

            addToExportHistory("complete", fromDate, toDate, exportId, finalZipFile.getName(), finalZipFile.length());

            log.info("[{}] ✅ COMPLETE SYSTEM EXPORT COMPLETED - File: {}", exportId, finalZipFile.getName());
            return finalZipFile;

        } finally {
            ongoingExports.remove(exportId);
        }
    }

    // ========== FILE EXPORT METHODS ==========

    private int exportDirectoryToZip(ZipOutputStream zos, String zipPrefix, String sourcePath,
                                     LocalDate fromDate, LocalDate toDate, String exportId) throws IOException {
        if (sourcePath == null || sourcePath.trim().isEmpty()) {
            log.warn("[{}] ❌ SKIPPING - Source path is null or empty: {}", exportId, zipPrefix);
            return 0;
        }

        Path sourceDir = Paths.get(sourcePath);
        if (!Files.exists(sourceDir)) {
            log.warn("[{}] ❌ SKIPPING - Source directory does not exist: {}", exportId, sourcePath);
            return 0;
        }

        if (!Files.isDirectory(sourceDir)) {
            log.warn("[{}] ❌ SKIPPING - Source path is not a directory: {}", exportId, sourcePath);
            return 0;
        }

        log.info("[{}] 📁 EXPORTING DIRECTORY: {} -> {}", exportId, sourcePath, zipPrefix);
        log.info("[{}] Date Range Filter: {} to {}", exportId, fromDate, toDate);

        final int[] fileCount = {0};
        try {
            Files.walk(sourceDir)
                    .filter(path -> !Files.isDirectory(path))
                    .filter(file -> {
                        // FIXED: If no date range specified, include ALL files
                        if (fromDate == null && toDate == null) {
                            return true; // Include ALL files when no date range
                        }
                        // Apply date range filtering only when dates are specified
                        return isFileInDateRange(file, fromDate, toDate);
                    })
                    .forEach(file -> {
                        try {
                            String relativePath = sourceDir.relativize(file).toString().replace("\\", "/");
                            String zipPath = zipPrefix + relativePath;

                            zos.putNextEntry(new ZipEntry(zipPath));
                            Files.copy(file, zos);
                            zos.closeEntry();

                            fileCount[0]++;

                            if (fileCount[0] % 50 == 0) {
                                log.debug("[{}] Added {} files from {} to ZIP", exportId, fileCount[0], zipPrefix);
                            }

                        } catch (IOException e) {
                            log.error("[{}] ❌ Error adding file {} to ZIP: {}", exportId, file, e.getMessage());
                        }
                    });

            log.info("[{}] ✅ SUCCESS - Exported {} files from {}", exportId, fileCount[0], zipPrefix);
            return fileCount[0];

        } catch (IOException e) {
            log.error("[{}] ❌ ERROR walking directory {}: {}", exportId, sourcePath, e.getMessage());
            return 0;
        }
    }

    private void exportFilesToDirectory(String exportDir, LocalDate fromDate, LocalDate toDate, String exportId) throws IOException {
        log.info("[{}] Exporting files to directory: {}", exportId, exportDir);
        log.info("[{}] Date Range Filter: {} to {}", exportId, fromDate, toDate);

        int totalFiles = 0;

        // Export documents - FIXED: Include ALL files when no date range
        totalFiles += copyDirectoryWithStructure(
                exportConfig.getDocumentStoragePath(),
                exportDir + "/documents",
                fromDate, toDate, exportId
        );

        // Export waiting room
        totalFiles += copyDirectoryWithStructure(
                exportConfig.getWaitingRoomStoragePath(),
                exportDir + "/waiting_room",
                fromDate, toDate, exportId
        );

        // Export profiles
        totalFiles += copyDirectoryWithStructure(
                exportConfig.getProfileStoragePath(),
                exportDir + "/profiles",
                fromDate, toDate, exportId
        );

        // Export archive
        totalFiles += copyDirectoryWithStructure(
                exportConfig.getDocumentArchivePath(),
                exportDir + "/archive",
                fromDate, toDate, exportId
        );

        // Create path mapping
        createPathMappingFile(exportDir, fromDate, toDate);
        createExportSummaryFile(exportDir, totalFiles, fromDate, toDate);

        log.info("[{}] ✅ DIRECTORY EXPORT COMPLETED - Total Files: {}", exportId, totalFiles);
    }

    private int copyDirectoryWithStructure(String sourcePath, String targetPath,
                                           LocalDate fromDate, LocalDate toDate, String exportId) throws IOException {
        if (sourcePath == null || !Files.exists(Paths.get(sourcePath))) {
            log.warn("[{}] ❌ SKIPPING - Source directory does not exist: {}", exportId, sourcePath);
            return 0;
        }

        Path sourceDir = Paths.get(sourcePath);
        Path targetDir = Paths.get(targetPath);

        log.info("[{}] 📂 COPYING: {} -> {}", exportId, sourcePath, targetPath);

        final int[] fileCount = {0};
        Files.walk(sourceDir)
                .filter(path -> !Files.isDirectory(path))
                .filter(file -> {
                    // FIXED: If no date range specified, include ALL files
                    if (fromDate == null && toDate == null) {
                        return true; // Include ALL files when no date range
                    }
                    // Apply date range filtering only when dates are specified
                    return isFileInDateRange(file, fromDate, toDate);
                })
                .forEach(file -> {
                    try {
                        Path relativePath = sourceDir.relativize(file);
                        Path targetFile = targetDir.resolve(relativePath);

                        Files.createDirectories(targetFile.getParent());
                        Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);

                        fileCount[0]++;

                        if (fileCount[0] % 50 == 0) {
                            log.debug("[{}] Copied {} files from {}", exportId, fileCount[0], sourcePath);
                        }
                    } catch (IOException e) {
                        log.error("[{}] ❌ Error copying file {}: {}", exportId, file, e.getMessage());
                    }
                });

        log.info("[{}] ✅ COPIED {} files from {}", exportId, fileCount[0], sourcePath);
        return fileCount[0];
    }

    // ========== DATE RANGE FILTERING ==========

    private boolean isFileInDateRange(Path file, LocalDate fromDate, LocalDate toDate) {
        try {
            LocalDateTime fileTime = Files.getLastModifiedTime(file).toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDateTime();
            LocalDate fileDate = fileTime.toLocalDate();

            // If both dates are null, include ALL files
            if (fromDate == null && toDate == null) {
                return true;
            }

            // Check if file is within date range
            boolean afterFrom = fromDate == null || !fileDate.isBefore(fromDate);
            boolean beforeTo = toDate == null || !fileDate.isAfter(toDate);

            return afterFrom && beforeTo;
        } catch (IOException e) {
            log.warn("Could not get modified time for file {}, including in export", file);
            return true; // Include file if we can't determine date
        }
    }

    // ========== DATABASE EXPORT METHODS ==========

    private void exportDatabaseToDirectory(String exportDir, LocalDate fromDate, LocalDate toDate,
                                           String exportId, DatabaseInfo dbInfo) throws Exception {
        List<String> tableNames = getTablesFromCurrentSchema(dbInfo);
        int tablesExported = 0;
        int totalRowsExported = 0;

        for (String tableName : tableNames) {
            ExportResult result = exportTableToCSV(tableName, exportDir, fromDate, toDate, exportId, dbInfo);
            if (result.isSuccess()) {
                tablesExported++;
                totalRowsExported += result.getRowCount();
            }
        }

        createMetadataFile(exportDir, tableNames, tablesExported, totalRowsExported, fromDate, toDate, dbInfo);
        log.info("[{}] Database exported to directory - Tables: {}/{}, Rows: {}",
                exportId, tablesExported, tableNames.size(), totalRowsExported);
    }

    private ExportResult exportTableToCSV(String tableName, String exportDir, LocalDate fromDate, LocalDate toDate, String exportId, DatabaseInfo dbInfo) {
        String csvFile = exportDir + "/" + tableName + ".csv";
        try {
            List<String> columns = getTableColumns(tableName, dbInfo);
            if (columns.isEmpty()) {
                try (FileWriter writer = new FileWriter(csvFile);
                     CSVWriter csvWriter = new CSVWriter(writer)) {
                    csvWriter.writeNext(new String[]{"no_columns_found"});
                }
                return new ExportResult(true, 0);
            }

            // Build query - FIXED: Only apply date filtering when dates are provided
            String query = buildQueryWithDateFilter(tableName, columns, fromDate, toDate);

            log.debug("[{}] Executing query for table {}: {}", exportId, tableName, query);

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(query);

            try (FileWriter writer = new FileWriter(csvFile);
                 CSVWriter csvWriter = new CSVWriter(writer)) {
                csvWriter.writeNext(columns.toArray(new String[0]));
                for (Map<String, Object> row : rows) {
                    String[] values = new String[columns.size()];
                    for (int i = 0; i < columns.size(); i++) {
                        Object value = row.get(columns.get(i));
                        values[i] = value != null ? value.toString() : "";
                    }
                    csvWriter.writeNext(values);
                }
            }

            log.info("[{}] Exported table {} with {} rows (Date Range: {} to {})",
                    exportId, tableName, rows.size(), fromDate, toDate);
            return new ExportResult(true, rows.size());

        } catch (Exception e) {
            log.error("[{}] Error exporting table {}: {}", exportId, tableName, e.getMessage());
            try (FileWriter writer = new FileWriter(csvFile);
                 CSVWriter csvWriter = new CSVWriter(writer)) {
                csvWriter.writeNext(new String[]{"export_error", e.getMessage()});
            } catch (Exception ex) {
                log.error("[{}] Failed to create error CSV for table {}: {}", exportId, tableName, ex.getMessage());
            }
            return new ExportResult(false, 0);
        }
    }

    private String buildQueryWithDateFilter(String tableName, List<String> columns, LocalDate fromDate, LocalDate toDate) {
        // FIXED: If no date range specified, return ALL data
        if (fromDate == null && toDate == null) {
            return "SELECT * FROM " + tableName;
        }

        // Look for date columns only when date range is specified
        List<String> dateColumns = Arrays.asList("created_date", "created_at", "updated_date",
                "update_date", "date_created", "timestamp", "upload_date", "modified_date",
                "creation_date", "date_modified", "created_on", "updated_on");

        String dateColumn = null;
        for (String col : columns) {
            if (dateColumns.contains(col.toLowerCase())) {
                dateColumn = col;
                break;
            }
        }

        // If no date column found but date range specified, return ALL data with warning
        if (dateColumn == null) {
            log.warn("No date column found for table {} but date range specified ({} to {}). Exporting ALL data.",
                    tableName, fromDate, toDate);
            return "SELECT * FROM " + tableName;
        }

        // Build date filter query
        StringBuilder query = new StringBuilder("SELECT * FROM " + tableName + " WHERE 1=1");

        if (fromDate != null) {
            query.append(" AND ").append(dateColumn).append(" >= '").append(fromDate).append("'");
        }
        if (toDate != null) {
            query.append(" AND ").append(dateColumn).append(" <= '").append(toDate.plusDays(1)).append("'");
        }

        log.debug("Using date-filtered query for table {}: {}", tableName, query);
        return query.toString();
    }

    // ========== DATABASE METHODS ==========

    private DatabaseInfo getCurrentDatabaseInfo() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String databaseName = connection.getCatalog();
            String databaseProductName = metaData.getDatabaseProductName();
            String databaseProductVersion = metaData.getDatabaseProductVersion();

            if (databaseName == null) {
                databaseName = connection.getSchema();
            }

            String databaseType = databaseProductName.contains("MySQL") ? DB_MYSQL :
                    databaseProductName.contains("PostgreSQL") ? DB_POSTGRESQL :
                            databaseProductName;

            return new DatabaseInfo(databaseName, databaseType, databaseProductVersion);
        }
    }

    private List<String> getTablesFromCurrentSchema(DatabaseInfo dbInfo) throws Exception {
        List<String> tableNames = new ArrayList<>();
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();

            String catalog = null;
            String schemaPattern = null;

            if (DB_MYSQL.equals(dbInfo.getDatabaseType())) {
                catalog = dbInfo.getDatabaseName();
            } else if (DB_POSTGRESQL.equals(dbInfo.getDatabaseType())) {
                schemaPattern = "public";
            }

            ResultSet tables = metaData.getTables(catalog, schemaPattern, "%", new String[]{"TABLE"});
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                if (shouldIncludeTable(tableName, dbInfo)) {
                    tableNames.add(tableName);
                }
            }
        }
        return tableNames;
    }

    private List<String> getTableColumns(String tableName, DatabaseInfo dbInfo) {
        List<String> columns = new ArrayList<>();
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();

            String catalog = null;
            String schemaPattern = null;

            if (DB_MYSQL.equals(dbInfo.getDatabaseType())) {
                catalog = dbInfo.getDatabaseName();
            } else if (DB_POSTGRESQL.equals(dbInfo.getDatabaseType())) {
                schemaPattern = "public";
            }

            ResultSet resultSet = metaData.getColumns(catalog, schemaPattern, tableName, null);
            while (resultSet.next()) {
                columns.add(resultSet.getString("COLUMN_NAME"));
            }
        } catch (Exception e) {
            log.error("Error getting columns for table {}: {}", tableName, e.getMessage());
        }
        return columns;
    }

    private boolean shouldIncludeTable(String tableName, DatabaseInfo dbInfo) {
        Set<String> systemTables = Set.of(
                "flyway_schema_history", "schema_version", "sys_config",
                "hibernate_sequence", "databasechangelog", "databasechangeloglock"
        );

        if (systemTables.contains(tableName.toLowerCase())) {
            return false;
        }

        if (tableName.toLowerCase().startsWith("qrtz_") ||
                tableName.toLowerCase().startsWith("act_") ||
                tableName.toLowerCase().startsWith("sys_") ||
                tableName.toLowerCase().startsWith("mysql_") ||
                tableName.toLowerCase().startsWith("pg_")) {
            return false;
        }

        return true;
    }

    private String getDateRangeSuffix(LocalDate fromDate, LocalDate toDate) {
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
        if (fromDate != null && toDate != null) {
            return fromDate.format(DATE_RANGE_FORMATTER) + "_to_" +
                    toDate.format(DATE_RANGE_FORMATTER) + "_" + timestamp;
        } else if (fromDate != null) {
            return "from_" + fromDate.format(DATE_RANGE_FORMATTER) + "_" + timestamp;
        } else if (toDate != null) {
            return "to_" + toDate.format(DATE_RANGE_FORMATTER) + "_" + timestamp;
        }
        return "full_export_" + timestamp;
    }

    private void addPathMappingToZip(ZipOutputStream zos, LocalDate fromDate, LocalDate toDate) throws IOException {
        zos.putNextEntry(new ZipEntry("PATH_MAPPING.txt"));
        zos.write(buildPathMappingContent(fromDate, toDate).getBytes());
        zos.closeEntry();
    }

    private void addExportSummaryToZip(ZipOutputStream zos, int totalFiles, LocalDate fromDate, LocalDate toDate) throws IOException {
        zos.putNextEntry(new ZipEntry("EXPORT_SUMMARY.txt"));
        zos.write(buildExportSummaryContent(totalFiles, fromDate, toDate).getBytes());
        zos.closeEntry();
    }

    private void createPathMappingFile(String exportDir, LocalDate fromDate, LocalDate toDate) throws IOException {
        String mappingFile = exportDir + "/PATH_MAPPING.txt";
        try (PrintWriter writer = new PrintWriter(new FileWriter(mappingFile))) {
            writer.write(buildPathMappingContent(fromDate, toDate));
        }
    }

    private void createExportSummaryFile(String exportDir, int totalFiles, LocalDate fromDate, LocalDate toDate) throws IOException {
        String summaryFile = exportDir + "/EXPORT_SUMMARY.txt";
        try (PrintWriter writer = new PrintWriter(new FileWriter(summaryFile))) {
            writer.write(buildExportSummaryContent(totalFiles, fromDate, toDate));
        }
    }

    private String buildPathMappingContent(LocalDate fromDate, LocalDate toDate) {
        return "DMS STORAGE PATH MAPPING - IMPORTANT FOR RESTORATION\n" +
                "====================================================\n" +
                "Export Time: " + LocalDateTime.now() + "\n" +
                "Date Range: " + formatDateRange(fromDate, toDate) + "\n" +
                "\nSTORAGE PATHS:\n" +
                "documents/    -> " + exportConfig.getDocumentStoragePath() + "\n" +
                "waiting_room/ -> " + exportConfig.getWaitingRoomStoragePath() + "\n" +
                "profiles/     -> " + exportConfig.getProfileStoragePath() + "\n" +
                "archive/      -> " + exportConfig.getDocumentArchivePath() + "\n" +
                "\nRESTORATION INSTRUCTIONS:\n" +
                "1. Extract ALL files maintaining the folder structure\n" +
                "2. Update application.properties with above paths\n" +
                "3. Ensure all directories exist before import\n" +
                "4. Import will restore files to these exact paths\n";
    }

    private String buildExportSummaryContent(int totalFiles, LocalDate fromDate, LocalDate toDate) {
        return "DMS FILES EXPORT SUMMARY\n" +
                "=======================\n" +
                "Export Time: " + LocalDateTime.now() + "\n" +
                "Date Range: " + formatDateRange(fromDate, toDate) + "\n" +
                "Total Files Exported: " + totalFiles + "\n" +
                "\nSTORAGE LOCATIONS INCLUDED:\n" +
                "✅ documents/    - " + exportConfig.getDocumentStoragePath() + "\n" +
                "✅ waiting_room/ - " + exportConfig.getWaitingRoomStoragePath() + "\n" +
                "✅ profiles/     - " + exportConfig.getProfileStoragePath() + "\n" +
                "✅ archive/      - " + exportConfig.getDocumentArchivePath() + "\n" +
                "\nEXPORT STATUS: COMPLETED SUCCESSFULLY ✅";
    }

    private void createMetadataFile(String exportDir, List<String> tableNames, int tablesExported,
                                    int totalRowsExported, LocalDate fromDate, LocalDate toDate, DatabaseInfo dbInfo) throws IOException {
        String metadataFile = exportDir + "/export_metadata.txt";
        try (PrintWriter writer = new PrintWriter(new FileWriter(metadataFile))) {
            writer.println("DMS DATABASE EXPORT METADATA");
            writer.println("============================");
            writer.println("Database: " + dbInfo.getDatabaseName());
            writer.println("Database Type: " + dbInfo.getDatabaseType());
            writer.println("Database Version: " + dbInfo.getDatabaseVersion());
            writer.println("Export Time: " + LocalDateTime.now());
            writer.println("Date Range: " + formatDateRange(fromDate, toDate));
            writer.println("Total Tables Found: " + tableNames.size());
            writer.println("Tables Successfully Exported: " + tablesExported);
            writer.println("Total Rows Exported: " + totalRowsExported);
            writer.println("Tables: " + String.join(", ", tableNames));
            writer.println("\nEXPORT MODE: " + (fromDate == null && toDate == null ? "FULL DATABASE EXPORT" : "DATE RANGE EXPORT"));
        }
    }

    private void createCompleteMetadataFile(String exportDir, LocalDate fromDate, LocalDate toDate, DatabaseInfo dbInfo) throws IOException {
        String metadataFile = exportDir + "/complete_metadata.txt";
        try (PrintWriter writer = new PrintWriter(new FileWriter(metadataFile))) {
            writer.println("DMS COMPLETE SYSTEM EXPORT");
            writer.println("===========================");
            writer.println("Database: " + dbInfo.getDatabaseName());
            writer.println("Database Type: " + dbInfo.getDatabaseType());
            writer.println("Export Time: " + LocalDateTime.now());
            writer.println("Date Range: " + formatDateRange(fromDate, toDate));
            writer.println("Export Mode: " + (fromDate == null && toDate == null ? "FULL SYSTEM EXPORT" : "DATE RANGE EXPORT"));
            writer.println("\nINCLUDES:");
            writer.println("✅ Database - All tables as CSV files");
            writer.println("✅ Files - All storage locations with original structure");
            writer.println("✅ Path Mapping - Configuration for restoration");
            writer.println("✅ Metadata - Export information and instructions");
            writer.println("\nSTRUCTURE:");
            writer.println("database_export/ - All database tables");
            writer.println("files_export/    - All files organized by type");
            writer.println("PATH_MAPPING.txt - Storage path configuration");
            writer.println("complete_metadata.txt - This file");
        }
    }

    private String formatDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null) {
            return fromDate + " to " + toDate;
        } else if (fromDate != null) {
            return "From " + fromDate + " onwards";
        } else if (toDate != null) {
            return "Up to " + toDate;
        }
        return "ALL DATA (No date range filter)";
    }

    private File createZipFile(String sourceDir, String zipFileName) throws IOException {
        Path sourcePath = Paths.get(sourceDir);
        try (FileOutputStream fos = new FileOutputStream(zipFileName);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            Files.walk(sourcePath)
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(file -> {
                        try {
                            String zipEntryName = sourcePath.relativize(file).toString().replace("\\", "/");
                            zos.putNextEntry(new ZipEntry(zipEntryName));
                            Files.copy(file, zos);
                            zos.closeEntry();
                        } catch (IOException e) {
                            log.error("Error adding file to ZIP: {}", e.getMessage());
                        }
                    });
        }

        // Cleanup temporary directory
        deleteDirectory(Paths.get(sourceDir));

        return new File(zipFileName);
    }

    private void deleteDirectory(Path path) {
        try {
            Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            log.warn("Error cleaning up directory {}: {}", path, e.getMessage());
        }
    }

    private String getDatabaseUrl() {
        try {
            return jdbcTemplate.getDataSource().getConnection().getMetaData().getURL();
        } catch (Exception e) {
            return "Unknown";
        }
    }

    // ========== DUPLICATE EXPORT VALIDATION ==========

    private void validateExportNotInProgress(String exportId, String type) {
        for (Map.Entry<String, ExportStatus> entry : ongoingExports.entrySet()) {
            ExportStatus status = entry.getValue();
            if (status.getType().equals(type) &&
                    Objects.equals(status.getFromDate(), status.getFromDate()) &&
                    Objects.equals(status.getToDate(), status.getToDate())) {
                throw new IllegalStateException(
                        String.format("%s export with date range %s to %s is already in progress.",
                                type, status.getFromDate(), status.getToDate()));
            }
        }
    }

    private void validateDuplicateExport(LocalDate fromDate, LocalDate toDate, String type, String exportId) {
        // FIXED: Only validate if both dates are provided
        if (fromDate == null || toDate == null) {
            log.debug("[{}] Skipping duplicate check - date range not fully specified: from={}, to={}",
                    exportId, fromDate, toDate);
            return;
        }

        List<ExportHistory> existingExports = getExportHistoryByType(type);

        log.debug("[{}] Checking duplicates for {} export - Date Range: {} to {}",
                exportId, type, fromDate, toDate);
        log.debug("[{}] Found {} existing exports of type {}", exportId, existingExports.size(), type);

        for (ExportHistory history : existingExports) {
            // FIXED: Skip history items with null dates
            if (history.getFromDate() == null || history.getToDate() == null) {
                log.debug("[{}] Skipping history item with null dates: from={}, to={}",
                        exportId, history.getFromDate(), history.getToDate());
                continue;
            }

            log.debug("[{}] Comparing with existing export: {} to {}",
                    exportId, history.getFromDate(), history.getToDate());

            if (isDateRangeCompletelyWithin(fromDate, toDate, history.getFromDate(), history.getToDate())) {
                log.warn("[{}] ❌ DUPLICATE EXPORT BLOCKED - {} export for {} to {} is within existing export {} to {}",
                        exportId, type, fromDate, toDate, history.getFromDate(), history.getToDate());
                throw new IllegalStateException(
                        String.format("Export blocked: Duplicate %s backup detected! Date range %s to %s is completely within existing %s backup %s to %s. " +
                                        "Please select a different date range.",
                                type, fromDate, toDate, type, history.getFromDate(), history.getToDate()));
            }
        }

        log.debug("[{}] ✅ No duplicates found for {} export", exportId, type);
    }

    private List<ExportHistory> getExportHistoryByType(String type) {
        return exportHistory.stream()
                .filter(history -> history.getType().equals(type))
                .toList();
    }

    private boolean isDateRangeCompletelyWithin(LocalDate newFrom, LocalDate newTo,
                                                LocalDate existingFrom, LocalDate existingTo) {
        // FIXED: Add null checks to prevent NullPointerException
        if (newFrom == null || newTo == null || existingFrom == null || existingTo == null) {
            return false; // If any date is null, they can't be compared
        }

        boolean completelyWithin = (newFrom.isAfter(existingFrom) || newFrom.isEqual(existingFrom)) &&
                (newTo.isBefore(existingTo) || newTo.isEqual(existingTo));
        boolean exactlySame = newFrom.isEqual(existingFrom) && newTo.isEqual(existingTo);

        return completelyWithin || exactlySame;
    }

    private void addToExportHistory(String type, LocalDate fromDate, LocalDate toDate,
                                    String exportId, String fileName, long fileSize) {
        // FIXED: Only add to history if we have valid export information
        if (fileName == null || fileSize == 0) {
            log.warn("[{}] Skipping history addition - invalid export data: fileName={}, fileSize={}",
                    exportId, fileName, fileSize);
            return;
        }

        ExportHistory history = new ExportHistory(
                exportId,
                type,
                fromDate,  // This can be null for Quick Backup
                toDate,    // This can be null for Quick Backup
                fileName,
                fileSize,
                LocalDateTime.now()
        );
        exportHistory.add(history);

        // Keep only last 50 exports to prevent memory issues
        if (exportHistory.size() > 50) {
            exportHistory.remove(0);
        }

        log.info("[{}] Added to {} export history: from {} to {}, file: {} ({} bytes)",
                exportId, type, fromDate, toDate, fileName, fileSize);
    }

    public List<ExportHistory> getExportHistoryForFrontend() {
        return exportHistory.stream()
                .sorted((h1, h2) -> h2.getExportTime().compareTo(h1.getExportTime()))
                .limit(10)
                .toList();
    }

    public List<ExportHistory> getExportHistoryByTypeForFrontend(String type) {
        return exportHistory.stream()
                .filter(history -> history.getType().equals(type))
                .sorted((h1, h2) -> h2.getExportTime().compareTo(h1.getExportTime()))
                .limit(10)
                .toList();
    }

    // ========== INNER CLASSES ==========

    private static class ExportResult {
        private final boolean success;
        private final int rowCount;

        public ExportResult(boolean success, int rowCount) {
            this.success = success;
            this.rowCount = rowCount;
        }

        public boolean isSuccess() { return success; }
        public int getRowCount() { return rowCount; }
    }

    private static class ExportStatus {
        private final String type;
        private final LocalDate fromDate;
        private final LocalDate toDate;
        private final LocalDateTime startTime;

        public ExportStatus(String type, LocalDate fromDate, LocalDate toDate) {
            this.type = type;
            this.fromDate = fromDate;
            this.toDate = toDate;
            this.startTime = LocalDateTime.now();
        }

        public String getType() { return type; }
        public LocalDate getFromDate() { return fromDate; }
        public LocalDate getToDate() { return toDate; }
        public LocalDateTime getStartTime() { return startTime; }
    }

    private static class DatabaseInfo {
        private final String databaseName;
        private final String databaseType;
        private final String databaseVersion;

        public DatabaseInfo(String databaseName, String databaseType, String databaseVersion) {
            this.databaseName = databaseName;
            this.databaseType = databaseType;
            this.databaseVersion = databaseVersion;
        }

        public String getDatabaseName() { return databaseName; }
        public String getDatabaseType() { return databaseType; }
        public String getDatabaseVersion() { return databaseVersion; }
    }

    public static class ExportHistory {
        private final String exportId;
        private final String type;
        private final LocalDate fromDate;
        private final LocalDate toDate;
        private final String fileName;
        private final long fileSize;
        private final LocalDateTime exportTime;

        public ExportHistory(String exportId, String type, LocalDate fromDate, LocalDate toDate,
                             String fileName, long fileSize, LocalDateTime exportTime) {
            this.exportId = exportId;
            this.type = type;
            this.fromDate = fromDate;
            this.toDate = toDate;
            this.fileName = fileName;
            this.fileSize = fileSize;
            this.exportTime = exportTime;
        }

        public String getExportId() { return exportId; }
        public String getType() { return type; }
        public LocalDate getFromDate() { return fromDate; }
        public LocalDate getToDate() { return toDate; }
        public String getFileName() { return fileName; }
        public long getFileSize() { return fileSize; }
        public LocalDateTime getExportTime() { return exportTime; }
    }
}