package com.dmsBackend.response;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MasTemplateRequest {
    private String templateCode;
    private String templateName;
}