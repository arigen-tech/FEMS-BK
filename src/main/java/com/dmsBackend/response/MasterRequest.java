// MasterRequest.java — add this one field to whatever your real file already has
package com.dmsBackend.response;

import lombok.Data;

@Data
public class MasterRequest {
    private String name;
    private Integer parentId;
    private String code;
    private Integer defaultParcelConditionId; // NEW — used only by SealStatusMaster
}