package com.dmsBackend.response;

public enum DownloadType {
    DATA_ONLY, FILES_ONLY, DATA_WITH_FILES;

    public boolean isFileIncluded() {
        return this == FILES_ONLY || this == DATA_WITH_FILES;
    }
}
