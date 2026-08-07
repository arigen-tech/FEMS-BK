package com.dmsBackend.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class TemplateApplicationResponse {
    private Long id;
    private Long templateId;
    private String appId;
    private String appName;
    private String status;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
    private Instant lastChgDate;
    private Long lastChgBy;
    private Long orderNo;
    private String parentId;



}
