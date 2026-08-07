package com.dmsBackend.response;

import com.dmsBackend.entity.BranchMaster;
import lombok.Data;
import java.sql.Timestamp;


@Data


public class DepartmentResponse {

    private Integer id;

    private String name;

    private Timestamp createdOn;

    private Timestamp updatedOn;

    private int isActive;

    private BranchMaster branch;

}
