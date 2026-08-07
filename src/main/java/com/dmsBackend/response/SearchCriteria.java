package com.dmsBackend.response;

import lombok.Data;
import org.apache.tika.metadata.filter.MetadataFilter;

import java.util.List;

@Data

public class SearchCriteria {

    private String fileNo;
    private String title;
    private String subject;
    private String version;

    private Integer categoryId;
    private Integer branchId;
    private Integer departmentId;
    private Integer yearId;

    // 🔥 METADATA FILTERS
    private List<DocumentSaveRequest.MetadataRequest> metadata;

    private int page;
    private int size;
}


