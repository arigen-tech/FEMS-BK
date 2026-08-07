package com.dmsBackend.response;

import lombok.Data;

@Data
public class FileResponse {
    private String fileName;
    private byte[] fileContent;
}
