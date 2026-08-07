package com.dmsBackend.response;

import lombok.Data;

import java.util.List;

@Data
public class ExternalDocumentSaveResponse {
    private Integer documentHeaderId;
    private List<Integer> documentDetailIds;
    private String status;
    private String message;
}
