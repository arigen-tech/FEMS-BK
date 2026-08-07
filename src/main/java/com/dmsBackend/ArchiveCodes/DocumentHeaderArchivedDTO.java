package com.dmsBackend.ArchiveCodes;

import com.dmsBackend.entity.BranchMaster;
import com.dmsBackend.entity.CategoryMaster;
import com.dmsBackend.entity.DepartmentMaster;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocumentHeaderArchivedDTO {
    private String uniqueId; // random unique id
    private String fileNo;
    private String title;
    private CategoryMaster categoryMaster;
    private BranchMaster branchName;
    private DepartmentMaster departmentName;
    private Integer documentHeaderId;
    private Long archiveJobId;
    private List<String> versions; // grouped versions
    private Integer files;
    private String status;
}

