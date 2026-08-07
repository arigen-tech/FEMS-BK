package com.dmsBackend.service;

import com.dmsBackend.config.ExportConfiguration;
import com.dmsBackend.response.ImportResponse;
import com.opencsv.CSVReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import java.io.*;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImportService {

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private final ExportConfiguration exportConfig;

    private final Map<String, ImportStatus> ongoingImports = new ConcurrentHashMap<>();
    private static final long MAX_FILE_SIZE = 100L * 1024 * 1024 * 1024;

    // ======================= IMPORT DMS EXPORT =======================
    public ImportResponse importDMSExport(MultipartFile file, boolean importDatabase,
                                          boolean importFiles, boolean overwriteExisting,
                                          Set<String> selectedTables, Set<String> selectedFiles) {

        String importId = UUID.randomUUID().toString();
        log.info("API CALL → Import DMS Export | importId={} database={} files={} overwrite={} tables={} selectedFiles={}",
                importId, importDatabase, importFiles, overwriteExisting,
                selectedTables != null ? selectedTables.size() : 0,
                selectedFiles != null ? selectedFiles.size() : 0);

        if (!importDatabase && !importFiles) {
            log.info("FAILED → Import DMS Export | importId={} reason=Nothing to import", importId);
            throw new IllegalArgumentException("Nothing to import - both database and files import are disabled");
        }

        validateImportNotInProgress(importId);

        try {
            ongoingImports.put(importId, new ImportStatus(importDatabase, importFiles));

            ImportResponse response = new ImportResponse();
            response.setImportId(importId);
            response.setTimestamp(LocalDateTime.now());

            if (file.getSize() > MAX_FILE_SIZE) {
                log.info("FAILED → Import DMS Export | importId={} reason=File size exceeds limit", importId);
                throw new IllegalArgumentException("File size exceeds maximum limit of 100GB");
            }

            String tempDir = exportConfig.getTempDirectory() + "/import_" + importId;
            Files.createDirectories(Paths.get(tempDir));
            log.info("Created temporary directory: {}", tempDir);

            Map<String, String> extractedFiles = extractZipFile(file, tempDir);
            log.info("Extracted {} files from ZIP", extractedFiles.size());

            Map<String, String> pathMapping = readPathMapping(extractedFiles);
            ImportMetadata metadata = readImportMetadata(extractedFiles, file.getOriginalFilename());

            response.setPathMappings(pathMapping);
            response.setMetadata(metadata);

            Map<String, Object> details = new HashMap<>();
            int recordsAdded = 0;
            int recordsUpdated = 0;
            int duplicateRecords = 0;

            if (importDatabase) {
                if (selectedTables == null || selectedTables.isEmpty()) {
                    Set<String> availableTables = detectAvailableTables(extractedFiles);
                    if (availableTables.isEmpty()) {
                        log.info("FAILED → Import DMS Export | importId={} reason=No tables found", importId);
                        throw new IllegalArgumentException("No tables selected for database import and no tables found in export");
                    } else {
                        selectedTables = availableTables;
                        log.info("Auto-selected all available tables: {}", selectedTables);
                    }
                }

                log.info("Starting database import for tables: {}", selectedTables);
                Map<String, Object> dbResult = importDatabaseFiles(extractedFiles, overwriteExisting, selectedTables);
                details.putAll(dbResult);
                response.setDatabaseImported(true);
                response.setDatabaseTables((Integer) dbResult.get("tablesImported"));
                response.setDatabaseRecords((Integer) dbResult.get("totalRecords"));

                @SuppressWarnings("unchecked")
                Set<String> importedTablesSet = (Set<String>) dbResult.get("importedTables");
                List<String> importedTablesList = new ArrayList<>(importedTablesSet);
                response.setSelectedTables(importedTablesList);

                recordsAdded = (Integer) dbResult.getOrDefault("recordsAdded", 0);
                recordsUpdated = (Integer) dbResult.getOrDefault("recordsUpdated", 0);
                duplicateRecords = (Integer) dbResult.getOrDefault("duplicateRecords", 0);

                log.info("Database import result - Tables: {}, Records: {}, Added: {}, Updated: {}, Duplicates: {}",
                        dbResult.get("tablesImported"), dbResult.get("totalRecords"),
                        recordsAdded, recordsUpdated, duplicateRecords);
            } else {
                response.setDatabaseImported(false);
                response.setDatabaseTables(0);
                response.setDatabaseRecords(0);
            }

            if (importFiles) {
                log.info("Starting file import for categories: {}", selectedFiles);
                Map<String, Object> fileResult = importFilesFromExport(extractedFiles, pathMapping, overwriteExisting, selectedFiles);
                details.putAll(fileResult);
                response.setFilesImported(true);
                response.setFilesImportedCount((Integer) fileResult.get("filesImported"));
                response.setFilesSkipped((Integer) fileResult.get("filesSkipped"));
                response.setFilesReplaced((Integer) fileResult.get("filesReplaced"));

                if (selectedFiles != null) {
                    response.setSelectedFiles(new ArrayList<>(selectedFiles));
                }
            } else {
                response.setFilesImported(false);
                response.setFilesImportedCount(0);
                response.setFilesSkipped(0);
                response.setFilesReplaced(0);
            }

            details.put("recordsAdded", recordsAdded);
            details.put("recordsUpdated", recordsUpdated);
            details.put("duplicateRecords", duplicateRecords);
            response.setDetails(details);

            if (!pathMapping.isEmpty()) {
                boolean pathsUpdated = updatePathConfiguration(pathMapping);
                response.setPathsUpdated(pathsUpdated);
            }

            response.setSuccess(true);
            response.setMessage("DMS import completed successfully");

            cleanupTempDirectory(tempDir);

            log.info("SUCCESS → DMS Import Completed | importId={} tables={} records={} added={} updated={} duplicates={}",
                    importId, response.getDatabaseTables(), response.getDatabaseRecords(),
                    recordsAdded, recordsUpdated, duplicateRecords);

            return response;

        } catch (Exception e) {
            log.error("FAILED → Import DMS Export | importId={} error={}", importId, e.getMessage(), e);
            ImportResponse errorResponse = new ImportResponse();
            errorResponse.setImportId(importId);
            errorResponse.setSuccess(false);
            errorResponse.setMessage("Import failed: " + e.getMessage());
            errorResponse.setError(e.getMessage());
            return errorResponse;
        } finally {
            ongoingImports.remove(importId);
        }
    }

    // ======================= DETECT AVAILABLE TABLES =======================
    private Set<String> detectAvailableTables(Map<String, String> extractedFiles) {
        log.debug("Detecting available tables from extracted files");

        Set<String> tables = new HashSet<>();
        for (String fileName : extractedFiles.keySet()) {
            if (fileName.toLowerCase().endsWith(".csv") &&
                    !fileName.toLowerCase().contains("metadata") &&
                    !fileName.contains("__MACOSX") &&
                    !fileName.contains(".DS_Store")) {
                String tableName = extractTableNameFromFileName(fileName);
                if (tableName != null && !tableName.isEmpty()) {
                    tables.add(tableName);
                    log.debug("Detected table: {} from file: {}", tableName, fileName);
                }
            }
        }

        log.info("Detected {} available tables: {}", tables.size(), tables);
        return tables;
    }

    // ======================= VALIDATE IMPORT NOT IN PROGRESS =======================
    private void validateImportNotInProgress(String importId) {
        if (!ongoingImports.isEmpty()) {
            log.info("FAILED → Import Validation | importId={} reason=Another import in progress", importId);
            throw new IllegalStateException("Another import operation is already in progress. Please wait for it to complete.");
        }
    }

    // ======================= EXTRACT ZIP FILE =======================
    private Map<String, String> extractZipFile(MultipartFile file, String tempDir) throws IOException {
        log.info("Extracting ZIP file to: {}", tempDir);

        Map<String, String> extractedFiles = new HashMap<>();

        try (ZipInputStream zis = new ZipInputStream(file.getInputStream())) {
            ZipEntry zipEntry;
            byte[] buffer = new byte[8192];

            while ((zipEntry = zis.getNextEntry()) != null) {
                String fileName = zipEntry.getName();

                if (fileName.contains("__MACOSX") || fileName.contains(".DS_Store")) {
                    zis.closeEntry();
                    continue;
                }

                File newFile = new File(tempDir + File.separator + fileName);

                if (zipEntry.isDirectory()) {
                    if (!newFile.isDirectory() && !newFile.mkdirs()) {
                        throw new IOException("Failed to create directory: " + newFile);
                    }
                } else {
                    File parent = newFile.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("Failed to create directory: " + parent);
                    }

                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }

                    extractedFiles.put(fileName, newFile.getAbsolutePath());
                    log.debug("Extracted file: {}", fileName);
                }
                zis.closeEntry();
            }
        }

        log.info("Extracted {} files from ZIP", extractedFiles.size());
        return extractedFiles;
    }

    // ======================= READ PATH MAPPING =======================
    private Map<String, String> readPathMapping(Map<String, String> extractedFiles) {
        log.debug("Reading path mapping from extracted files");

        Map<String, String> pathMapping = new HashMap<>();

        for (Map.Entry<String, String> entry : extractedFiles.entrySet()) {
            if (entry.getKey().equals("PATH_MAPPING.txt")) {
                try (BufferedReader reader = new BufferedReader(new FileReader(entry.getValue()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains("=")) {
                            String[] parts = line.split("=", 2);
                            if (parts.length == 2) {
                                String key = parts[0].trim();
                                String value = parts[1].trim();
                                if (!value.isEmpty()) {
                                    pathMapping.put(key, value);
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    log.warn("Could not read path mapping file: {}", e.getMessage());
                }
                break;
            }
        }

        log.info("Read path mapping with {} entries", pathMapping.size());
        return pathMapping;
    }

    // ======================= READ IMPORT METADATA =======================
    private ImportMetadata readImportMetadata(Map<String, String> extractedFiles, String zipFileName) {
        log.debug("Reading import metadata from extracted files");

        ImportMetadata metadata = new ImportMetadata();
        boolean metadataFound = false;
        boolean hasDatabaseFiles = false;
        boolean hasFileSystemFiles = false;

        if (zipFileName != null) {
            Pattern zipDatePattern = Pattern.compile(".*_(\\d{8})_to_(\\d{8}).*", Pattern.CASE_INSENSITIVE);
            Matcher matcher = zipDatePattern.matcher(zipFileName);
            if (matcher.matches()) {
                String fromDate = matcher.group(1);
                String toDate = matcher.group(2);
                try {
                    DateTimeFormatter inputFmt = DateTimeFormatter.ofPattern("yyyyMMdd");
                    DateTimeFormatter outputFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                    String formattedRange = LocalDate.parse(fromDate, inputFmt).format(outputFmt)
                            + " to " +
                            LocalDate.parse(toDate, inputFmt).format(outputFmt);
                    metadata.setDateRange(formattedRange);
                    log.info("Date range extracted from ZIP filename: {}", formattedRange);
                } catch (Exception e) {
                    log.warn("Could not parse date range from ZIP filename '{}': {}", zipFileName, e.getMessage());
                }
            } else {
                log.info("No date pattern found in ZIP filename: {}", zipFileName);
            }
        }

        for (String fileName : extractedFiles.keySet()) {
            if (fileName.endsWith(".csv") && !fileName.contains("metadata")) {
                hasDatabaseFiles = true;
            }
            if (fileName.startsWith("documents/") || fileName.startsWith("waiting_room/") ||
                    fileName.startsWith("profiles/") || fileName.startsWith("archive/")) {
                hasFileSystemFiles = true;
            }

            if (fileName.endsWith(".csv") && fileName.contains("_")) {
                extractDateFromFileName(fileName, metadata);
            }
        }

        log.info("Content analysis - Database files: {}, File system files: {}",
                hasDatabaseFiles, hasFileSystemFiles);

        for (Map.Entry<String, String> entry : extractedFiles.entrySet()) {
            String fileName = entry.getKey();

            if (fileName.toLowerCase().contains("metadata") &&
                    (fileName.endsWith(".txt") || fileName.endsWith(".TXT")) &&
                    !fileName.contains("__MACOSX") &&
                    !fileName.contains(".DS_Store")) {

                log.info("Reading metadata from file: {}", fileName);
                metadataFound = true;

                try (BufferedReader reader = new BufferedReader(new FileReader(entry.getValue()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty()) continue;

                        if ((line.toLowerCase().contains("database:") || line.toLowerCase().contains("source:")) &&
                                !line.toLowerCase().contains("database type:")) {
                            String value = extractValueAfterColon(line);
                            if (value != null && !value.equalsIgnoreCase("unknown")) {
                                metadata.setDatabaseName(value);
                            }
                        } else if (line.toLowerCase().contains("database type:")) {
                            String value = extractValueAfterColon(line);
                            if (value != null) metadata.setDatabaseType(value);
                        } else if (line.toLowerCase().contains("export time:") ||
                                line.toLowerCase().contains("export date:") ||
                                line.toLowerCase().contains("exported:") ||
                                line.toLowerCase().contains("created:")) {
                            String value = extractValueAfterColon(line);
                            if (value != null) metadata.setExportTime(value);
                        } else if (line.toLowerCase().contains("date range:") ||
                                line.toLowerCase().contains("period:") ||
                                line.toLowerCase().contains("time range:") ||
                                line.toLowerCase().contains("data period:") ||
                                line.toLowerCase().contains("export period:")) {
                            String value = extractValueAfterColon(line);
                            if (value != null && !value.equalsIgnoreCase("unknown")) {
                                metadata.setDateRange(value);
                            }
                        } else if (line.toLowerCase().contains("total tables:") ||
                                line.toLowerCase().contains("tables exported:") ||
                                line.toLowerCase().contains("tables:")) {
                            try {
                                String value = extractValueAfterColon(line);
                                if (value != null && value.matches("\\d+")) {
                                    metadata.setTotalTables(Integer.parseInt(value));
                                }
                            } catch (NumberFormatException e) {
                                log.warn("Could not parse total tables: {}", line);
                            }
                        }
                    }
                    log.info("Successfully processed metadata file: {}", fileName);
                } catch (Exception e) {
                    log.warn("Error reading metadata file {}: {}", fileName, e.getMessage());
                }
            }
        }

        if (metadataFound && metadata.getDateRange() == null && metadata.getExportTime() != null) {
            String derivedRange = deriveDateRangeFromExportTime(metadata.getExportTime());
            if (derivedRange != null) {
                metadata.setDateRange(derivedRange);
            }
        }

        if (metadata.getDateRange() == null) {
            String inferredRange = inferDateRangeFromFileNames(extractedFiles.keySet());
            if (inferredRange != null) {
                metadata.setDateRange(inferredRange);
            }
        }

        if (metadata.getDatabaseName() == null) {
            if (hasDatabaseFiles && hasFileSystemFiles) {
                metadata.setDatabaseName("DMS Complete System");
            } else if (hasDatabaseFiles) {
                metadata.setDatabaseName("DMS Database");
            } else if (hasFileSystemFiles) {
                metadata.setDatabaseName("DMS File System");
            } else {
                metadata.setDatabaseName("DMS Archive");
            }
        }

        if (metadata.getDateRange() == null) {
            if (hasDatabaseFiles && hasFileSystemFiles) {
                metadata.setDateRange("Complete system backup");
            } else if (hasDatabaseFiles) {
                metadata.setDateRange("Database snapshot");
            } else if (hasFileSystemFiles) {
                metadata.setDateRange("File system backup");
            } else {
                metadata.setDateRange("System data export");
            }

            String currentDate = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            metadata.setDateRange(metadata.getDateRange() + " - " + currentDate);
        }

        log.info("Final metadata - Source: '{}', Period: '{}', Export Time: '{}'",
                metadata.getDatabaseName(), metadata.getDateRange(), metadata.getExportTime());

        return metadata;
    }

    // ======================= EXTRACT VALUE AFTER COLON =======================
    private String extractValueAfterColon(String line) {
        if (line.contains(":")) {
            String value = line.split(":", 2)[1].trim();
            return value.isEmpty() ? null : value;
        }
        return null;
    }

    // ======================= DERIVE DATE RANGE FROM EXPORT TIME =======================
    private String deriveDateRangeFromExportTime(String exportTime) {
        try {
            Pattern pattern = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
            Matcher matcher = pattern.matcher(exportTime);
            if (matcher.find()) {
                String exportDate = matcher.group();
                return "Export: " + exportDate;
            }
        } catch (Exception e) {
            log.debug("Could not derive date range from export time: {}", e.getMessage());
        }
        return null;
    }

    // ======================= INFER DATE RANGE FROM FILE NAMES =======================
    private String inferDateRangeFromFileNames(Set<String> fileNames) {
        try {
            Set<String> dates = new TreeSet<>();

            for (String fileName : fileNames) {
                Pattern pattern = Pattern.compile(
                        "\\d{4}[-_]\\d{2}[-_]\\d{2}"
                );
                Matcher matcher = pattern.matcher(fileName);
                if (matcher.find()) {
                    String date = matcher.group().replace("_", "-");
                    dates.add(date);
                }
            }

            if (!dates.isEmpty()) {
                List<String> dateList = new ArrayList<>(dates);
                if (dateList.size() == 1) {
                    return dateList.get(0);
                } else {
                    return dateList.get(0) + " to " + dateList.get(dateList.size() - 1);
                }
            }
        } catch (Exception e) {
            log.debug("Could not infer date range from file names: {}", e.getMessage());
        }
        return null;
    }

    // ======================= EXTRACT DATE FROM FILE NAME =======================
    private void extractDateFromFileName(String fileName, ImportMetadata metadata) {
        try {
            Pattern pattern = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
            Matcher matcher = pattern.matcher(fileName);
            if (matcher.find()) {
                String date = matcher.group();
                if (metadata.getDateRange() == null) {
                    metadata.setDateRange("Data from " + date);
                }
            }
        } catch (Exception e) {
            // Ignore errors in date extraction
        }
    }

    // ======================= IMPORT DATABASE FILES =======================
    private Map<String, Object> importDatabaseFiles(Map<String, String> extractedFiles,
                                                    boolean overwriteExisting,
                                                    Set<String> selectedTables) {
        log.info("Starting database files import | overwrite={} tables={}",
                overwriteExisting, selectedTables != null ? selectedTables.size() : 0);

        Map<String, Object> result = new HashMap<>();
        int tablesImported = 0;
        int totalRecords = 0;
        int recordsAdded = 0;
        int recordsUpdated = 0;
        int duplicateRecords = 0;
        Set<String> importedTables = new HashSet<>();

        try {
            List<String> csvFiles = new ArrayList<>();
            for (String fileName : extractedFiles.keySet()) {
                if (fileName.toLowerCase().endsWith(".csv") &&
                        !fileName.toLowerCase().contains("metadata") &&
                        !fileName.contains("__MACOSX") &&
                        !fileName.contains(".DS_Store")) {

                    String tableName = extractTableNameFromFileName(fileName);
                    if (tableName != null && !tableName.isEmpty()) {
                        csvFiles.add(tableName);
                        log.debug("Found CSV file: {} -> Table: {}", fileName, tableName);
                    }
                }
            }

            log.info("Found {} CSV files for import: {}", csvFiles.size(), csvFiles);
            log.info("Selected tables for import: {}", selectedTables);

            if (csvFiles.isEmpty()) {
                log.error("No CSV files found in the export");
                throw new IllegalArgumentException("No database tables found in the export file");
            }

            List<String> tablesToImport = new ArrayList<>();
            for (String table : csvFiles) {
                if (selectedTables.contains(table)) {
                    tablesToImport.add(table);
                    log.info("Table {} selected for import", table);
                }
            }

            if (tablesToImport.isEmpty()) {
                log.error("No matching tables found for selected tables: {}", selectedTables);
                throw new IllegalArgumentException("No matching tables found for selected tables: " + selectedTables);
            }

            log.info("Importing {} selected tables: {}", tablesToImport.size(), tablesToImport);

            for (String tableName : tablesToImport) {
                String csvFilePath = findCsvFilePath(extractedFiles, tableName);
                if (csvFilePath != null) {
                    try {
                        log.info("Starting import for table: {} from file: {}", tableName, csvFilePath);
                        TableImportResult tableResult = importCsvToTable(tableName, csvFilePath, overwriteExisting);
                        if (tableResult.getTotalRecords() > 0 || tableResult.getDuplicateCount() > 0) {
                            tablesImported++;
                            totalRecords += tableResult.getTotalRecords();
                            recordsAdded += tableResult.getRecordsAdded();
                            recordsUpdated += tableResult.getRecordsUpdated();
                            duplicateRecords += tableResult.getDuplicateCount();
                            importedTables.add(tableName);
                            log.info("Successfully processed table {} - Total: {}, Added: {}, Updated: {}, Duplicates: {}",
                                    tableName, tableResult.getTotalRecords(),
                                    tableResult.getRecordsAdded(), tableResult.getRecordsUpdated(),
                                    tableResult.getDuplicateCount());
                        } else {
                            log.warn("No records processed for table: {}", tableName);
                        }
                    } catch (Exception e) {
                        log.error("Failed to import table {}: {}", tableName, e.getMessage(), e);
                        throw new RuntimeException("Failed to import table " + tableName + ": " + e.getMessage(), e);
                    }
                } else {
                    log.error("CSV file not found for table: {}", tableName);
                    throw new RuntimeException("CSV file not found for table: " + tableName);
                }
            }

            result.put("tablesImported", tablesImported);
            result.put("totalRecords", totalRecords);
            result.put("recordsAdded", recordsAdded);
            result.put("recordsUpdated", recordsUpdated);
            result.put("duplicateRecords", duplicateRecords);
            result.put("importedTables", importedTables);
            result.put("overwrite", overwriteExisting);
            result.put("availableTables", csvFiles);

            log.info("SUCCESS → Database Import Completed | tables={} records={} added={} updated={} duplicates={}",
                    tablesImported, totalRecords, recordsAdded, recordsUpdated, duplicateRecords);

        } catch (Exception e) {
            log.error("FAILED → Database Import | error={}", e.getMessage(), e);
            throw new RuntimeException("Database import failed: " + e.getMessage(), e);
        }

        return result;
    }

    // ======================= EXTRACT TABLE NAME FROM FILE NAME =======================
    private String extractTableNameFromFileName(String fileName) {
        try {
            String simpleFileName = fileName;
            if (fileName.contains("/")) {
                simpleFileName = fileName.substring(fileName.lastIndexOf("/") + 1);
            }

            if (simpleFileName.toLowerCase().endsWith(".csv")) {
                return simpleFileName.substring(0, simpleFileName.length() - 4);
            }

            return simpleFileName;
        } catch (Exception e) {
            log.warn("Error extracting table name from {}: {}", fileName, e.getMessage());
            return fileName;
        }
    }

    // ======================= FIND CSV FILE PATH =======================
    private String findCsvFilePath(Map<String, String> extractedFiles, String tableName) {
        String csvFileName = tableName + ".csv";
        String filePath = extractedFiles.get(csvFileName);

        if (filePath != null) {
            log.debug("Found exact CSV match: {} -> {}", csvFileName, filePath);
            return filePath;
        }

        for (Map.Entry<String, String> entry : extractedFiles.entrySet()) {
            String fileName = entry.getKey();
            if (fileName.toLowerCase().endsWith(".csv") &&
                    !fileName.toLowerCase().contains("metadata")) {

                String currentTableName = extractTableNameFromFileName(fileName);
                if (currentTableName != null && currentTableName.equalsIgnoreCase(tableName)) {
                    log.debug("Found case-insensitive CSV match: {} -> {}", fileName, entry.getValue());
                    return entry.getValue();
                }
            }
        }

        log.warn("CSV file not found for table: {} (searched for: {})", tableName, csvFileName);
        return null;
    }

    // ======================= TABLE EXISTS =======================
    private boolean tableExists(String tableName) {
        try {
            String sql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName);
            boolean exists = count != null && count > 0;
            log.debug("Table {} exists: {}", tableName, exists);
            return exists;
        } catch (Exception e) {
            log.error("Error checking if table {} exists: {}", tableName, e.getMessage());
            return false;
        }
    }

    // ======================= IS MYSQL =======================
    private boolean isMySQL() {
        try {
            String url = dataSource.getConnection().getMetaData().getURL();
            boolean isMySQL = url.contains("mysql");
            log.debug("Database is MySQL: {}", isMySQL);
            return isMySQL;
        } catch (Exception e) {
            log.error("Error determining database type: {}", e.getMessage());
            return false;
        }
    }

    // ======================= IMPORT CSV TO TABLE =======================
    private TableImportResult importCsvToTable(String tableName, String csvFilePath, boolean overwriteExisting) {
        log.info("Importing CSV to table: {} from file: {}", tableName, csvFilePath);

        int totalRecords = 0;
        int recordsAdded = 0;
        int recordsUpdated = 0;
        int duplicateCount = 0;

        if (!tableExists(tableName)) {
            log.error("Table {} does not exist in the database", tableName);
            return new TableImportResult(0, 0, 0, 0);
        }

        try (CSVReader reader = new CSVReader(new FileReader(csvFilePath))) {
            String[] headers = reader.readNext();
            if (headers == null || headers.length == 0) {
                log.warn("Empty CSV file for table: {}", tableName);
                return new TableImportResult(0, 0, 0, 0);
            }

            log.info("CSV headers for table {}: {}", tableName, Arrays.toString(headers));

            List<ColumnInfo> actualColumns = getTableColumnInfo(tableName);
            if (actualColumns.isEmpty()) {
                log.error("Table {} has no columns or cannot be accessed", tableName);
                return new TableImportResult(0, 0, 0, 0);
            }

            log.info("Database columns for table {}: {}", tableName, actualColumns);

            List<String> validHeaders = new ArrayList<>();
            List<Integer> validIndexes = new ArrayList<>();
            List<Integer> columnTypes = new ArrayList<>();

            for (int i = 0; i < headers.length; i++) {
                String header = headers[i].trim();
                for (ColumnInfo column : actualColumns) {
                    if (column.getName().equalsIgnoreCase(header)) {
                        validHeaders.add(header);
                        validIndexes.add(i);
                        columnTypes.add(column.getDataType());
                        log.debug("Including column: {} with type: {}", header, column.getDataType());
                        break;
                    }
                }
            }

            if (validHeaders.isEmpty()) {
                log.error("No matching columns found between CSV and table {}: CSV={}, Table={}",
                        tableName, Arrays.toString(headers), actualColumns);
                return new TableImportResult(0, 0, 0, 0);
            }

            log.info("Valid columns for import: {}", validHeaders);

            List<String> primaryKeys = getPrimaryKeyColumns(tableName);
            boolean hasPrimaryKey = !primaryKeys.isEmpty();
            log.info("Primary keys for table {}: {}", tableName, primaryKeys);

            String sql;
            if (overwriteExisting && hasPrimaryKey && isMySQL()) {
                sql = buildUpsertStatement(tableName, validHeaders, primaryKeys);
                log.info("Using UPSERT (ON DUPLICATE KEY UPDATE) for table: {}", tableName);
            } else if (!overwriteExisting && hasPrimaryKey && isMySQL()) {
                sql = buildInsertIgnoreStatement(tableName, validHeaders);
                log.info("Using INSERT IGNORE to skip duplicates for table: {}", tableName);
            } else if (overwriteExisting && hasPrimaryKey) {
                sql = buildMergeStatement(tableName, validHeaders, primaryKeys);
                log.info("Using MERGE for table: {}", tableName);
            } else {
                sql = buildInsertStatement(tableName, validHeaders);
                log.info("Using simple INSERT for table: {} (may fail on duplicates)", tableName);
            }

            log.info("Using SQL for table {}: {}", tableName, sql);

            String[] nextLine;
            int batchSize = 50;
            List<Object[]> batchArgs = new ArrayList<>();
            int lineNumber = 1;
            int successCount = 0;

            while ((nextLine = reader.readNext()) != null) {
                lineNumber++;
                if (nextLine.length >= validIndexes.size()) {
                    try {
                        Object[] params = prepareParameters(nextLine, validIndexes, columnTypes);
                        if (params != null) {
                            batchArgs.add(params);
                            successCount++;
                        }

                        if (batchArgs.size() >= batchSize) {
                            BatchResult batchResult = processBatch(sql, batchArgs, overwriteExisting, hasPrimaryKey);
                            totalRecords += batchResult.getProcessedRecords();
                            recordsAdded += batchResult.getRecordsAdded();
                            recordsUpdated += batchResult.getRecordsUpdated();
                            duplicateCount += batchResult.getDuplicateCount();
                            batchArgs.clear();

                            if (totalRecords % 50 == 0) {
                                log.info("Processed {} records for table {} so far (Added: {}, Updated: {}, Duplicates: {})",
                                        totalRecords, tableName, recordsAdded, recordsUpdated, duplicateCount);
                            }
                        }
                    } catch (Exception e) {
                        log.warn("Failed to prepare record at line {} for insertion into {}: {}",
                                lineNumber, tableName, e.getMessage());
                    }
                } else {
                    log.warn("Skipping line {} - insufficient columns: expected at least {}, got {}",
                            lineNumber, validIndexes.size(), nextLine.length);
                }
            }

            if (!batchArgs.isEmpty()) {
                BatchResult batchResult = processBatch(sql, batchArgs, overwriteExisting, hasPrimaryKey);
                totalRecords += batchResult.getProcessedRecords();
                recordsAdded += batchResult.getRecordsAdded();
                recordsUpdated += batchResult.getRecordsUpdated();
                duplicateCount += batchResult.getDuplicateCount();
            }

            log.info("Completed processing for table {} - Total: {}, Added: {}, Updated: {}, Duplicates: {}, Successfully prepared: {}",
                    tableName, totalRecords, recordsAdded, recordsUpdated, duplicateCount, successCount);

        } catch (Exception e) {
            log.error("Error importing CSV to table {}: {}", tableName, e.getMessage(), e);
            throw new RuntimeException("Failed to import table " + tableName + ": " + e.getMessage(), e);
        }

        return new TableImportResult(totalRecords, recordsAdded, recordsUpdated, duplicateCount);
    }

    // ======================= BUILD INSERT IGNORE STATEMENT =======================
    private String buildInsertIgnoreStatement(String tableName, List<String> columns) {
        String placeholders = String.join(",", Collections.nCopies(columns.size(), "?"));
        String columnList = String.join(",", columns);
        return String.format("INSERT IGNORE INTO %s (%s) VALUES (%s)", tableName, columnList, placeholders);
    }

    // ======================= PREPARE PARAMETERS =======================
    private Object[] prepareParameters(String[] nextLine, List<Integer> validIndexes, List<Integer> columnTypes) {
        Object[] params = new Object[validIndexes.size()];

        for (int i = 0; i < validIndexes.size(); i++) {
            int idx = validIndexes.get(i);
            int columnType = columnTypes.get(i);

            if (idx < nextLine.length && nextLine[idx] != null && !nextLine[idx].isEmpty()) {
                String value = nextLine[idx].trim();
                try {
                    params[i] = convertValueForType(value, columnType);
                } catch (Exception e) {
                    log.warn("Failed to convert value '{}' for column type {}: {}", value, columnType, e.getMessage());
                    params[i] = null;
                }
            } else {
                params[i] = null;
            }
        }
        return params;
    }

    // ======================= CONVERT VALUE FOR TYPE =======================
    private Object convertValueForType(String value, int columnType) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        try {
            switch (columnType) {
                case Types.BIT:
                case Types.BOOLEAN:
                    if (value.equalsIgnoreCase("true") || value.equals("1") || value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("y")) {
                        return true;
                    } else if (value.equalsIgnoreCase("false") || value.equals("0") || value.equalsIgnoreCase("no") || value.equalsIgnoreCase("n")) {
                        return false;
                    } else {
                        try {
                            return Integer.parseInt(value) != 0;
                        } catch (NumberFormatException e) {
                            return Boolean.parseBoolean(value);
                        }
                    }

                case Types.TINYINT:
                case Types.SMALLINT:
                case Types.INTEGER:
                    try {
                        return Integer.parseInt(value);
                    } catch (NumberFormatException e) {
                        if (value.contains(".")) {
                            return (int) Double.parseDouble(value);
                        }
                        throw e;
                    }

                case Types.BIGINT:
                    try {
                        return Long.parseLong(value);
                    } catch (NumberFormatException e) {
                        if (value.contains(".")) {
                            return (long) Double.parseDouble(value);
                        }
                        throw e;
                    }

                case Types.FLOAT:
                    return Float.parseFloat(value);

                case Types.DOUBLE:
                case Types.DECIMAL:
                case Types.NUMERIC:
                    return Double.parseDouble(value);

                case Types.DATE:
                case Types.TIME:
                case Types.TIMESTAMP:
                    return value;

                default:
                    return value;
            }
        } catch (Exception e) {
            log.warn("Conversion failed for value '{}' to type {}: {}", value, columnType, e.getMessage());
            return value;
        }
    }

    // ======================= PROCESS BATCH =======================
    private BatchResult processBatch(String sql, List<Object[]> batchArgs, boolean overwriteExisting, boolean hasPrimaryKey) {
        int recordsAdded = 0;
        int recordsUpdated = 0;
        int duplicateCount = 0;

        try {
            int[] batchResult = jdbcTemplate.batchUpdate(sql, batchArgs);

            if (overwriteExisting && hasPrimaryKey && isMySQL() && sql.toUpperCase().contains("ON DUPLICATE KEY UPDATE")) {
                for (int result : batchResult) {
                    if (result == 1) {
                        recordsAdded++;
                    } else if (result == 2) {
                        recordsUpdated++;
                    } else {
                        log.warn("Unexpected batch result: {}", result);
                    }
                }
            } else if (!overwriteExisting && hasPrimaryKey && isMySQL() && sql.toUpperCase().contains("INSERT IGNORE")) {
                for (int result : batchResult) {
                    if (result == 1) {
                        recordsAdded++;
                    } else if (result == 0) {
                        duplicateCount++;
                    } else {
                        log.warn("Unexpected batch result for INSERT IGNORE: {}", result);
                    }
                }
            } else {
                recordsAdded = batchArgs.size();
            }

            log.debug("Batch processed - Total: {}, Added: {}, Updated: {}, Duplicates: {}",
                    batchArgs.size(), recordsAdded, recordsUpdated, duplicateCount);

            return new BatchResult(batchArgs.size(), recordsAdded, recordsUpdated, duplicateCount);

        } catch (Exception e) {
            log.error("Error processing batch with SQL {}: {}", sql, e.getMessage());

            if (e.getMessage().contains("Duplicate entry") && !overwriteExisting) {
                log.info("Duplicate records found and skipped due to overwriteExisting=false");
                return new BatchResult(0, 0, 0, batchArgs.size());
            }

            for (int i = 0; i < Math.min(batchArgs.size(), 3); i++) {
                log.error("Sample parameters [{}]: {}", i, Arrays.toString(batchArgs.get(i)));
            }

            return new BatchResult(0, 0, 0, 0);
        }
    }

    // ======================= BUILD INSERT STATEMENT =======================
    private String buildInsertStatement(String tableName, List<String> columns) {
        String placeholders = String.join(",", Collections.nCopies(columns.size(), "?"));
        String columnList = String.join(",", columns);
        return String.format("INSERT INTO %s (%s) VALUES (%s)", tableName, columnList, placeholders);
    }

    // ======================= BUILD UPSERT STATEMENT =======================
    private String buildUpsertStatement(String tableName, List<String> columns, List<String> primaryKeys) {
        String placeholders = String.join(",", Collections.nCopies(columns.size(), "?"));
        String columnList = String.join(",", columns);

        List<String> updateParts = new ArrayList<>();
        for (String column : columns) {
            if (!primaryKeys.contains(column.toLowerCase())) {
                updateParts.add(column + " = VALUES(" + column + ")");
            }
        }

        if (updateParts.isEmpty()) {
            return String.format("INSERT IGNORE INTO %s (%s) VALUES (%s)", tableName, columnList, placeholders);
        }

        String updates = String.join(", ", updateParts);
        return String.format("INSERT INTO %s (%s) VALUES (%s) ON DUPLICATE KEY UPDATE %s",
                tableName, columnList, placeholders, updates);
    }

    // ======================= BUILD MERGE STATEMENT =======================
    private String buildMergeStatement(String tableName, List<String> columns, List<String> primaryKeys) {
        if (isPostgreSQL()) {
            return buildPostgreSQLUpsert(tableName, columns, primaryKeys);
        } else {
            return buildInsertStatement(tableName, columns);
        }
    }

    // ======================= BUILD POSTGRESQL UPSERT =======================
    private String buildPostgreSQLUpsert(String tableName, List<String> columns, List<String> primaryKeys) {
        String placeholders = String.join(",", Collections.nCopies(columns.size(), "?"));
        String columnList = String.join(",", columns);
        String conflictColumns = String.join(", ", primaryKeys);

        List<String> updateParts = new ArrayList<>();
        for (String column : columns) {
            if (!primaryKeys.contains(column.toLowerCase())) {
                updateParts.add(column + " = EXCLUDED." + column);
            }
        }

        if (updateParts.isEmpty()) {
            return String.format(
                    "INSERT INTO %s (%s) VALUES (%s) ON CONFLICT (%s) DO NOTHING",
                    tableName, columnList, placeholders, conflictColumns
            );
        }

        String updates = String.join(", ", updateParts);
        return String.format(
                "INSERT INTO %s (%s) VALUES (%s) ON CONFLICT (%s) DO UPDATE SET %s",
                tableName, columnList, placeholders, conflictColumns, updates
        );
    }

    // ======================= IS POSTGRESQL =======================
    private boolean isPostgreSQL() {
        try {
            String url = dataSource.getConnection().getMetaData().getURL();
            return url.contains("postgresql");
        } catch (Exception e) {
            return false;
        }
    }

    // ======================= GET TABLE COLUMN INFO =======================
    private List<ColumnInfo> getTableColumnInfo(String tableName) {
        List<ColumnInfo> columns = new ArrayList<>();
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet resultSet = metaData.getColumns(null, null, tableName, null);
            while (resultSet.next()) {
                String columnName = resultSet.getString("COLUMN_NAME");
                int dataType = resultSet.getInt("DATA_TYPE");
                if (columnName != null) {
                    columns.add(new ColumnInfo(columnName.toLowerCase(), dataType));
                }
            }
            log.debug("Found {} columns for table {}", columns.size(), tableName);
        } catch (Exception e) {
            log.error("Error getting columns for table {}: {}", tableName, e.getMessage());
        }
        return columns;
    }

    // ======================= GET PRIMARY KEY COLUMNS =======================
    private List<String> getPrimaryKeyColumns(String tableName) {
        List<String> primaryKeys = new ArrayList<>();
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet resultSet = metaData.getPrimaryKeys(null, null, tableName);
            while (resultSet.next()) {
                String columnName = resultSet.getString("COLUMN_NAME");
                if (columnName != null) {
                    primaryKeys.add(columnName.toLowerCase());
                }
            }
            log.debug("Found {} primary keys for table {}", primaryKeys.size(), tableName);
        } catch (Exception e) {
            log.error("Error getting primary keys for table {}: {}", tableName, e.getMessage());
        }
        return primaryKeys;
    }

    // ======================= IMPORT FILES FROM EXPORT =======================
    private Map<String, Object> importFilesFromExport(Map<String, String> extractedFiles,
                                                      Map<String, String> pathMapping,
                                                      boolean overwriteExisting,
                                                      Set<String> selectedFileCategories) {
        log.info("Importing files from export | overwrite={} categories={}",
                overwriteExisting, selectedFileCategories != null ? selectedFileCategories.size() : 0);

        Map<String, Object> result = new HashMap<>();
        int filesImported = 0;
        int filesSkipped = 0;
        int filesReplaced = 0;
        Set<String> importedCategories = new HashSet<>();

        try {
            Map<String, String> pathTargets = Map.of(
                    "documents", getCurrentPath("document.storage.path", pathMapping),
                    "waiting_room", getCurrentPath("waitingroom.storage.path", pathMapping),
                    "profiles", getCurrentPath("ProfileStoragePath", pathMapping),
                    "archive", getCurrentPath("document.archive.path", pathMapping)
            );

            log.info("File import path targets: {}", pathTargets);
            log.info("Selected file categories for import: {}", selectedFileCategories);

            if (selectedFileCategories == null || selectedFileCategories.isEmpty()) {
                log.warn("No file categories selected, but file import requested. Will import all available files.");
            }

            for (Map.Entry<String, String> entry : extractedFiles.entrySet()) {
                String zipPath = entry.getKey();
                String sourcePath = entry.getValue();

                if (zipPath.equals("PATH_MAPPING.txt") ||
                        zipPath.endsWith("_metadata.txt") ||
                        zipPath.contains("__MACOSX") ||
                        zipPath.endsWith(".csv")) {
                    continue;
                }

                String targetPath = null;
                String relativePath = null;
                String fileCategory = extractCategoryFromPath(zipPath);

                if (selectedFileCategories != null && !selectedFileCategories.isEmpty() &&
                        fileCategory != null && !selectedFileCategories.contains(fileCategory)) {
                    log.debug("Skipping file from unselected category: {} - {}", fileCategory, zipPath);
                    continue;
                }

                if (fileCategory != null && pathTargets.containsKey(fileCategory)) {
                    String basePath = pathTargets.get(fileCategory);
                    if (zipPath.contains(fileCategory + "/")) {
                        relativePath = zipPath.substring(zipPath.indexOf(fileCategory + "/") + fileCategory.length() + 1);
                    } else {
                        relativePath = zipPath.substring(zipPath.indexOf(fileCategory) + fileCategory.length());
                        if (relativePath.startsWith("/") || relativePath.startsWith("\\")) {
                            relativePath = relativePath.substring(1);
                        }
                    }

                    if (relativePath != null && !relativePath.isEmpty()) {
                        targetPath = basePath + File.separator + relativePath;
                    }
                }

                if (targetPath != null && relativePath != null && fileCategory != null) {
                    FileImportResult importResult = copyFileToTarget(sourcePath, targetPath, overwriteExisting);
                    if (importResult.isImported()) {
                        filesImported++;
                        importedCategories.add(fileCategory);
                        if (importResult.isReplaced()) {
                            filesReplaced++;
                        }
                        log.debug("Successfully imported file: {} -> {}", zipPath, targetPath);
                    } else {
                        filesSkipped++;
                        log.debug("Skipped file: {}", zipPath);
                    }
                } else {
                    log.debug("Could not determine target path for: {}", zipPath);
                    filesSkipped++;
                }
            }

            result.put("filesImported", filesImported);
            result.put("filesSkipped", filesSkipped);
            result.put("filesReplaced", filesReplaced);
            result.put("importedCategories", importedCategories);
            result.put("selectedCategories", selectedFileCategories);

            log.info("SUCCESS → File Import Completed | imported={} replaced={} skipped={} categories={}",
                    filesImported, filesReplaced, filesSkipped, importedCategories);

        } catch (Exception e) {
            log.error("FAILED → File Import | error={}", e.getMessage(), e);
            throw new RuntimeException("File import failed: " + e.getMessage(), e);
        }

        return result;
    }

    // ======================= GET CURRENT PATH =======================
    private String getCurrentPath(String pathKey, Map<String, String> pathMapping) {
        String path = null;

        switch (pathKey) {
            case "document.storage.path":
                path = exportConfig.getDocumentStoragePath();
                break;
            case "waitingroom.storage.path":
                path = exportConfig.getWaitingRoomStoragePath();
                break;
            case "ProfileStoragePath":
                path = exportConfig.getProfileStoragePath();
                break;
            case "document.archive.path":
                path = exportConfig.getDocumentArchivePath();
                break;
        }

        if (path == null || path.trim().isEmpty()) {
            path = pathMapping.get(pathKey);
        }

        if (path != null) {
            try {
                Files.createDirectories(Paths.get(path));
                log.info("Ensured directory exists: {}", path);
            } catch (IOException e) {
                log.warn("Could not create directory {}: {}", path, e.getMessage());
            }
        }

        return path;
    }

    // ======================= COPY FILE TO TARGET =======================
    private FileImportResult copyFileToTarget(String sourcePath, String targetPath, boolean overwrite) {
        try {
            Path source = Paths.get(sourcePath);
            Path target = Paths.get(targetPath);

            Files.createDirectories(target.getParent());

            boolean fileExists = Files.exists(target);
            boolean replaced = false;

            if (fileExists) {
                if (overwrite) {
                    Files.delete(target);
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                    replaced = true;
                    log.debug("Replaced existing file: {}", targetPath);
                } else {
                    log.debug("Skipping existing file: {}", targetPath);
                    return new FileImportResult(false, false);
                }
            } else {
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                log.debug("Copied new file to: {}", targetPath);
            }

            return new FileImportResult(true, replaced);

        } catch (Exception e) {
            log.error("Error copying file from {} to {}: {}", sourcePath, targetPath, e.getMessage());
            return new FileImportResult(false, false);
        }
    }

    // ======================= UPDATE PATH CONFIGURATION =======================
    private boolean updatePathConfiguration(Map<String, String> pathMapping) {
        log.info("Path mapping recommendations for application.properties:");
        for (Map.Entry<String, String> entry : pathMapping.entrySet()) {
            log.info("{}={}", entry.getKey(), entry.getValue());
        }

        return true;
    }

    // ======================= CLEANUP TEMP DIRECTORY =======================
    private void cleanupTempDirectory(String tempDir) {
        try {
            Path path = Paths.get(tempDir);
            if (Files.exists(path)) {
                Files.walk(path)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
                log.info("Cleaned up temporary directory: {}", tempDir);
            }
        } catch (IOException e) {
            log.warn("Could not clean up temporary directory {}: {}", tempDir, e.getMessage());
        }
    }

    // ======================= VALIDATE IMPORT FILE =======================
    public ImportResponse validateImportFile(MultipartFile file) {
        log.info("API CALL → Validate Import File | fileName={} size={}",
                file.getOriginalFilename(), file.getSize());

        ImportResponse response = new ImportResponse();
        response.setImportId(UUID.randomUUID().toString());
        response.setTimestamp(LocalDateTime.now());

        try {
            if (!file.getOriginalFilename().toLowerCase().endsWith(".zip")) {
                log.info("FAILED → Validate Import File | fileName={} reason=Invalid file type", file.getOriginalFilename());
                response.setSuccess(false);
                response.setMessage("Invalid file type. Please upload a ZIP file.");
                return response;
            }

            if (file.getSize() > MAX_FILE_SIZE) {
                log.info("FAILED → Validate Import File | fileName={} reason=File too large", file.getOriginalFilename());
                response.setSuccess(false);
                response.setMessage("File too large. Maximum size is 100GB.");
                return response;
            }

            String tempDir = exportConfig.getTempDirectory() + "/validate_" + UUID.randomUUID();
            Files.createDirectories(Paths.get(tempDir));

            try {
                Map<String, String> extractedFiles = extractZipFile(file, tempDir);

                boolean hasDatabase = false;
                boolean hasFiles = false;
                Set<String> availableTables = new HashSet<>();
                Set<String> availableFiles = new HashSet<>();
                Map<String, FileCategoryInfo> fileStructure = new HashMap<>();

                for (String fileName : extractedFiles.keySet()) {
                    log.debug("Analyzing file: {}", fileName);

                    if (fileName.contains("__MACOSX") || fileName.contains(".DS_Store")) {
                        continue;
                    }

                    if (fileName.toLowerCase().endsWith(".csv") &&
                            !fileName.toLowerCase().contains("metadata")) {
                        hasDatabase = true;
                        String tableName = extractTableNameFromFileName(fileName);
                        if (tableName != null && !tableName.isEmpty()) {
                            availableTables.add(tableName);
                            log.info("Found database table: {}", tableName);
                        }
                    }

                    if ((fileName.startsWith("documents/") || fileName.contains("/documents/") ||
                            fileName.startsWith("waiting_room/") || fileName.contains("/waiting_room/") ||
                            fileName.startsWith("profiles/") || fileName.contains("/profiles/") ||
                            fileName.startsWith("archive/") || fileName.contains("/archive/")) &&
                            !fileName.toLowerCase().endsWith(".csv") &&
                            !fileName.toLowerCase().endsWith(".txt")) {

                        hasFiles = true;
                        String category = extractCategoryFromPath(fileName);
                        if (category != null) {
                            availableFiles.add(category);
                            updateFileCategoryInfo(fileStructure, category, fileName, extractedFiles.get(fileName));
                        }
                        log.info("Found file system content: {} -> {}", fileName, category);
                    }

                    if ((fileName.startsWith("documents") || fileName.startsWith("waiting_room") ||
                            fileName.startsWith("profiles") || fileName.startsWith("archive")) &&
                            !fileName.contains("/") && !fileName.contains("\\") &&
                            !fileName.toLowerCase().endsWith(".csv") &&
                            !fileName.toLowerCase().endsWith(".txt")) {

                        hasFiles = true;
                        String category = extractCategoryFromPath(fileName);
                        if (category != null && !availableFiles.contains(category)) {
                            availableFiles.add(category);
                        }
                        log.info("Found root file: {} -> {}", fileName, category);
                    }
                }

                Map<String, Object> details = new HashMap<>();
                details.put("hasDatabase", hasDatabase);
                details.put("hasFiles", hasFiles);
                details.put("fileSize", file.getSize());
                details.put("fileName", file.getOriginalFilename());
                details.put("availableTables", new ArrayList<>(availableTables));
                details.put("availableFiles", new ArrayList<>(availableFiles));
                details.put("fileStructure", fileStructure);
                details.put("totalFilesFound", extractedFiles.size());

                log.info("=== FILE VALIDATION RESULTS ===");
                log.info("File: {}, Size: {}", file.getOriginalFilename(), file.getSize());
                log.info("Total files in ZIP: {}", extractedFiles.size());
                log.info("Has Database: {}, Tables: {}", hasDatabase, availableTables);
                log.info("Has Files: {}, Categories: {}", hasFiles, availableFiles);
                log.info("Extracted files: {}", extractedFiles.keySet());

                response.setDetails(details);
                response.setSuccess(true);
                response.setMessage("File validation successful - " +
                        (hasDatabase ? "Database found (" + availableTables.size() + " tables) " : "") +
                        (hasFiles ? "Files found (" + availableFiles.size() + " categories)" : ""));

                log.info("SUCCESS → File Validation Completed | fileName={} database={} files={} tables={} categories={}",
                        file.getOriginalFilename(), hasDatabase, hasFiles, availableTables.size(), availableFiles.size());

            } finally {
                cleanupTempDirectory(tempDir);
            }

        } catch (Exception e) {
            log.error("FAILED → File Validation | fileName={} error={}", file.getOriginalFilename(), e.getMessage(), e);
            response.setSuccess(false);
            response.setMessage("File validation failed: " + e.getMessage());
            response.setError(e.getMessage());
        }

        return response;
    }

    // ======================= UPDATE FILE CATEGORY INFO =======================
    private void updateFileCategoryInfo(Map<String, FileCategoryInfo> fileStructure, String category, String fileName, String filePath) {
        FileCategoryInfo info = fileStructure.getOrDefault(category, new FileCategoryInfo());
        info.setFileCount(info.getFileCount() + 1);

        try {
            File file = new File(filePath);
            if (file.exists()) {
                info.setTotalSize(info.getTotalSize() + file.length());
            }
        } catch (Exception e) {
            log.debug("Could not get file size for {}: {}", fileName, e.getMessage());
        }

        fileStructure.put(category, info);
    }

    // ======================= EXTRACT CATEGORY FROM PATH =======================
    private String extractCategoryFromPath(String entryName) {
        if (entryName.contains("documents")) return "documents";
        if (entryName.contains("waiting_room")) return "waiting_room";
        if (entryName.contains("profiles")) return "profiles";
        if (entryName.contains("archive")) return "archive";
        return null;
    }

    // ======================= HELPER CLASSES =======================
    private static class ImportStatus {
        private final boolean importDatabase;
        private final boolean importFiles;
        private final LocalDateTime startTime;

        public ImportStatus(boolean importDatabase, boolean importFiles) {
            this.importDatabase = importDatabase;
            this.importFiles = importFiles;
            this.startTime = LocalDateTime.now();
        }
    }

    private static class FileImportResult {
        private final boolean imported;
        private final boolean replaced;

        public FileImportResult(boolean imported, boolean replaced) {
            this.imported = imported;
            this.replaced = replaced;
        }

        public boolean isImported() { return imported; }
        public boolean isReplaced() { return replaced; }
    }

    private static class TableImportResult {
        private final int totalRecords;
        private final int recordsAdded;
        private final int recordsUpdated;
        private final int duplicateCount;

        public TableImportResult(int totalRecords, int recordsAdded, int recordsUpdated, int duplicateCount) {
            this.totalRecords = totalRecords;
            this.recordsAdded = recordsAdded;
            this.recordsUpdated = recordsUpdated;
            this.duplicateCount = duplicateCount;
        }

        public int getTotalRecords() { return totalRecords; }
        public int getRecordsAdded() { return recordsAdded; }
        public int getRecordsUpdated() { return recordsUpdated; }
        public int getDuplicateCount() { return duplicateCount; }
    }

    private static class BatchResult {
        private final int processedRecords;
        private final int recordsAdded;
        private final int recordsUpdated;
        private final int duplicateCount;

        public BatchResult(int processedRecords, int recordsAdded, int recordsUpdated, int duplicateCount) {
            this.processedRecords = processedRecords;
            this.recordsAdded = recordsAdded;
            this.recordsUpdated = recordsUpdated;
            this.duplicateCount = duplicateCount;
        }

        public int getProcessedRecords() { return processedRecords; }
        public int getRecordsAdded() { return recordsAdded; }
        public int getRecordsUpdated() { return recordsUpdated; }
        public int getDuplicateCount() { return duplicateCount; }
    }

    public static class ImportMetadata {
        private String databaseName;
        private String databaseType;
        private String exportTime;
        private String dateRange;
        private int totalTables;

        public String getDatabaseName() { return databaseName; }
        public void setDatabaseName(String databaseName) { this.databaseName = databaseName; }
        public String getDatabaseType() { return databaseType; }
        public void setDatabaseType(String databaseType) { this.databaseType = databaseType; }
        public String getExportTime() { return exportTime; }
        public void setExportTime(String exportTime) { this.exportTime = exportTime; }
        public String getDateRange() { return dateRange; }
        public void setDateRange(String dateRange) { this.dateRange = dateRange; }
        public int getTotalTables() { return totalTables; }
        public void setTotalTables(int totalTables) { this.totalTables = totalTables; }
    }

    private static class ColumnInfo {
        private final String name;
        private final int dataType;

        public ColumnInfo(String name, int dataType) {
            this.name = name;
            this.dataType = dataType;
        }

        public String getName() { return name; }
        public int getDataType() { return dataType; }

        @Override
        public String toString() {
            return name + "(" + dataType + ")";
        }
    }

    public static class FileCategoryInfo {
        private int fileCount;
        private long totalSize;

        public FileCategoryInfo() {
            this.fileCount = 0;
            this.totalSize = 0;
        }

        public int getFileCount() { return fileCount; }
        public void setFileCount(int fileCount) { this.fileCount = fileCount; }
        public long getTotalSize() { return totalSize; }
        public void setTotalSize(long totalSize) { this.totalSize = totalSize; }
    }
}