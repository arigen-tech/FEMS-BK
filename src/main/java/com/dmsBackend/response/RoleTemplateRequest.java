package com.dmsBackend.response;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoleTemplateRequest {
    private Long roleId;
    private Long templateId;
    private String status;
    private Long lastChgBy;
}
