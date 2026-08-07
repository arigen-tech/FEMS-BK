package com.dmsBackend.response;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AdvancedDownloadByIdsRequest {

    @NotEmpty
    private List<Integer> documentIds;

    @NotNull
    private DownloadType downloadType;  // DATA_ONLY | FILES_ONLY | DATA_WITH_FILES

    private boolean confirmFiles;
}
