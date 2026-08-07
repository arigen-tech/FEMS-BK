package com.dmsBackend.response;

import lombok.Data;

@Data

public class FilePathRequest {

    private String path;
    private String version;
    private Integer yearId;
    private Boolean isWaitingRoomFile;
    private Integer waitingRoomId;
    private String destinationPath;
    private String displayName;
}
