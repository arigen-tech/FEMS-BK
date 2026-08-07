package com.dmsBackend.response;

import lombok.Data;
import java.util.List;

@Data
public class FileCompareResponse {
    private boolean identical;
    private String message;
    private double similarityPercentage;
    private List<DifferenceHighlight> differences;
    private FileComparisonResult comparisonResult;
    private String diffImagePath;

    public FileCompareResponse() {}

    public FileCompareResponse(boolean identical, String message, double similarityPercentage) {
        this.identical = identical;
        this.message = message;
        this.similarityPercentage = similarityPercentage;
    }

    @Data
    public static class FileComparisonResult {
        private FileContent leftFile;
        private FileContent rightFile;
        private List<DifferenceHighlight> differences;
        private ComparisonSummary summary;

        public FileComparisonResult() {}

        public FileComparisonResult(FileContent leftFile, FileContent rightFile,
                                    List<DifferenceHighlight> differences, ComparisonSummary summary) {
            this.leftFile = leftFile;
            this.rightFile = rightFile;
            this.differences = differences;
            this.summary = summary;
        }
    }

    @Data
    public static class FileContent {
        private String fileName;
        private String version;
        private String filePath;
        private List<String> lines;
        private String fileType;
        private String content;           // Full text content
        private String highlightedContent; // Content with HTML highlighting

        public FileContent() {}

        public FileContent(String fileName, String version, String filePath,
                           List<String> lines, String fileType) {
            this.fileName = fileName;
            this.version = version;
            this.filePath = filePath;
            this.lines = lines;
            this.fileType = fileType;
            this.content = lines != null ? String.join("\n", lines) : "";
        }

        public FileContent(String fileName, String version, String filePath,
                           List<String> lines, String fileType, String content) {
            this.fileName = fileName;
            this.version = version;
            this.filePath = filePath;
            this.lines = lines;
            this.fileType = fileType;
            this.content = content;
        }
    }

    @Data
    public static class DifferenceHighlight {
        private int leftLineNumber;
        private int rightLineNumber;
        private String type;
        private String leftContent;
        private String rightContent;
        private String color;

        public DifferenceHighlight() {}

        public DifferenceHighlight(int leftLineNumber, int rightLineNumber, String type,
                                   String leftContent, String rightContent, String color) {
            this.leftLineNumber = leftLineNumber;
            this.rightLineNumber = rightLineNumber;
            this.type = type;
            this.leftContent = leftContent;
            this.rightContent = rightContent;
            this.color = color;
        }
    }

    @Data
    public static class ComparisonSummary {
        private int totalLinesAdded;
        private int totalLinesDeleted;
        private int totalLinesModified;
        private int totalDifferences;
        private double similarityPercentage;

        public ComparisonSummary() {}

        public ComparisonSummary(int totalLinesAdded, int totalLinesDeleted,
                                 int totalLinesModified, int totalDifferences,
                                 double similarityPercentage) {
            this.totalLinesAdded = totalLinesAdded;
            this.totalLinesDeleted = totalLinesDeleted;
            this.totalLinesModified = totalLinesModified;
            this.totalDifferences = totalDifferences;
            this.similarityPercentage = similarityPercentage;
        }
    }
}