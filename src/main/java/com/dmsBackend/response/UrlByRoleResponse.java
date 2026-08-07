package com.dmsBackend.response;

import java.util.List;

public class UrlByRoleResponse {
    private String appId;
    private String name;
    private String url;

    public Long getSerialNo() {
        return serialNo;
    }

    public void setSerialNo(Long serialNo) {
        this.serialNo = serialNo;
    }

    private Long serialNo;
    private List<UrlByRoleResponse> children;

    // Getters and setters
    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<UrlByRoleResponse> getChildren() {
        return children;
    }

    public void setChildren(List<UrlByRoleResponse> children) {
        this.children = children;
    }
}