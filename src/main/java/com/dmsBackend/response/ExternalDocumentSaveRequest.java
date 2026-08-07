package com.dmsBackend.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;


@Data
public class ExternalDocumentSaveRequest {

    private String title;
    private String fileNo;
    private String subject;
    private Integer categoryId;

    private List<MetadataRequest> metadata;

    @JsonIgnore
    private List<MultipartFile> files;

    private String year;
    private String version;

    @Data
    public static class MetadataRequest {
        private String key;
        private String value;
    }
}

