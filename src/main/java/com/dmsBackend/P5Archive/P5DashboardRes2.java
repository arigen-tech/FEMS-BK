package com.dmsBackend.P5Archive;

import lombok.*;

import java.time.LocalDateTime;

@Setter@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class P5DashboardRes2 {
    private Long id;
    private String docNumber;
    private String title;
    private String branchName;
    private Integer branchId;
    private String departmentName;
    private Integer departmentId;
    private Integer totalVersion;
    private Integer totalfiles;
}
