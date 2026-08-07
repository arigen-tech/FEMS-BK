package com.dmsBackend.response;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StatusCountByYearDto {
    private int year;
    private long activeCount;
    private long inactiveCount;
    private long pendingCount;
    private long totalCount;

    public StatusCountByYearDto(int year, long activeCount, long inactiveCount, long pendingCount) {
        this.year = year;
        this.activeCount = activeCount;
        this.inactiveCount = inactiveCount;
        this.pendingCount = pendingCount;
        this.totalCount = activeCount + inactiveCount + pendingCount;
    }


}
