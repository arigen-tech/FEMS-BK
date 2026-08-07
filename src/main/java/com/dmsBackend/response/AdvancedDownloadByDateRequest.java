package com.dmsBackend.response;


import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class AdvancedDownloadByDateRequest {

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fromDate;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate toDate;

    @NotNull
    private DownloadType downloadType;   // DATA_ONLY | FILES_ONLY | DATA_WITH_FILES

    private boolean confirmFiles;
}


