package com.dmsBackend.response;

import lombok.Data;

@Data
public class FileCompareRequest {
    private Integer firstFileId;
    private Integer secondFileId;

    public FileCompareRequest() {}

    public FileCompareRequest(String firstFilePath, String secondFilePath) {
        this.firstFileId = firstFileId;
        this.secondFileId = secondFileId;
    }
}
