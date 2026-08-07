package com.dmsBackend.response;

import org.springframework.core.io.InputStreamResource;

public class DownloadResult {
    private InputStreamResource resource;
    private String message;

    public DownloadResult(InputStreamResource resource, String message) {
        this.resource = resource;
        this.message = message;
    }

    public InputStreamResource getResource() {
        return resource;
    }

    public String getMessage() {
        return message;
    }

    public boolean hasResource() {
        return resource != null;
    }
}
