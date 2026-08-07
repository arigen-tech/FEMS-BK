package com.dmsBackend.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MasApplicationRequest {

    private String name;
    private String parentId;
    private String url;
    private String status;
    private Long serialNo;
}
