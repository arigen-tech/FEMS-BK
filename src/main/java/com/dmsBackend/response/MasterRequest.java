package com.dmsBackend.response;

import lombok.Data;

/**
 * One request DTO for every master type.
 * parentId is only used by District, City, and EvidenceType
 * (ignored for the simple masters).
 */
@Data
public class MasterRequest {

    private String name;

    private Integer parentId;

    private String code;
}
