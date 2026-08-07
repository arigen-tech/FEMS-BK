package com.dmsBackend.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
@Data
public class DocumentResponse2 {
    private String fileNo;
    private String title;
    private String subject;
    private List<FileInfo> fileList;

    public DocumentResponse2(String fileNo, String title, String subject, List<FileInfo> fileList) {
        this.fileNo = fileNo;
        this.title = title;
        this.subject = subject;
        this.fileList = fileList;
    }

    public String getFileNo() { return fileNo; }
    public String getTitle() { return title; }
    public String getSubject() { return subject; }
    public List<FileInfo> getFileList() { return fileList; }
    @Data
    @AllArgsConstructor
    public static class FileInfo {
        private String fileName;
        private String version;
        private String path;
        private Integer detailsId;
    }
}
