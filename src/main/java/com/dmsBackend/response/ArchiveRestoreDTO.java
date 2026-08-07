package com.dmsBackend.response;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArchiveRestoreDTO {
    private Integer branchId;
    private Integer departmentId;
    private String userRole;
    private MultipartFile file;
}
