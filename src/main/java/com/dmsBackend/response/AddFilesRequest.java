package com.dmsBackend.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class AddFilesRequest {

    private String year;
    private String version;

    @JsonIgnore
    private List<MultipartFile> files;
}
