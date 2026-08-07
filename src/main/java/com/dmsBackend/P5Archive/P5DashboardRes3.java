package com.dmsBackend.P5Archive;

import lombok.*;

import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class P5DashboardRes3 {
    private String  id;
    private String version;
    private String status;
    private LocalDateTime archivalDateTime;
    private LocalDateTime archivedDate;
    private Integer totalfiles;
}