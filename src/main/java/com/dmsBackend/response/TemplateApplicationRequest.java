package com.dmsBackend.response;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TemplateApplicationRequest {
    private Long templateId;
    private String appId;
    private Long lastChgBy;
    private Long orderNo;
    private String status;
}