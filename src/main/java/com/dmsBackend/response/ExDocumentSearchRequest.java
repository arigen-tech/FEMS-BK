package com.dmsBackend.response;

import lombok.Data;

import java.util.List;

@Data
public class ExDocumentSearchRequest {

    private String fileNo;
    private String title;
    private String subject;
    private String category;
    private String year;

    private List<MetadataFilter> metadata;

    private Integer page = 0;
    private Integer size = 20;
    private String sortBy = "fileNo";
    private String sortDir = "ASC";

    @Data
    public static class MetadataFilter {
        private String key;
        private String value;
    }
}
