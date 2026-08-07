package com.dmsBackend.response;


import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
public class MasApplicationResponse {
    private String appId;
    private String name;
    private String parentId;
    private String url;
    private Long orderNo;
    private String status;
    private Instant lastChgDate;
    private Long appSequenceNo;
    private Long serialNo;
    private boolean assigned;
    private List<MasApplicationResponse> children;
}
