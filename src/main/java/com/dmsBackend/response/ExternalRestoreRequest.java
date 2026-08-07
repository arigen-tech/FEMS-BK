package com.dmsBackend.response;


import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ExternalRestoreRequest {

    // MODE
    @NotNull
    private RestoreType restoreType; // BY_IDS or BY_DATE

    // BY IDS
    private List<Integer> headerIds;

    // BY DATE
    private LocalDate fromDate;
    private LocalDate toDate;



    public enum RestoreType {
        BY_IDS,
        BY_DATE
    }

}
