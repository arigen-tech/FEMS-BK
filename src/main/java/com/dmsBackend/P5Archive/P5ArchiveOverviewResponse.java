package com.dmsBackend.P5Archive;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class P5ArchiveOverviewResponse {

    @JsonProperty("Archive Overview")
    private List<ArchiveItem> archiveOverview;

    @Data
    public static class ArchiveItem {

        private String plan;

        @JsonProperty("start time")
        private Instant startTime;

        @JsonProperty("finish time")
        private Instant finishTime;

        private String status;
        private String sizeKbytes;
        private String client;
        private List<String> directories;
        private String pool;
    }
}
