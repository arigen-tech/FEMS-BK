package com.dmsBackend.P5Archive;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DashboardCountResponse {

    // Policy counts
    private Long totalPolicy;
    private Long archivedPolicy;
    private Long failedPolicy;
    private Long inProgressPolicy;
    private Long waitingPolicy;
    private Double archivedPercentByPolicy;

    // File counts
    private Long totalFiles;
    private Long archivedFiles;
    private Long failedFiles;
    private Long inProgressFiles;

    // getters & setters
}
