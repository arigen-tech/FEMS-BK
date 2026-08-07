package com.dmsBackend.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FileTypeCountDTO {
    private int year;
    private String fileType;
    private long fileCount;

    public FileTypeCountDTO(int year, String fileType, long fileCount) {
        this.year = year;
        this.fileType = fileType;
        this.fileCount = fileCount;
    }

}
