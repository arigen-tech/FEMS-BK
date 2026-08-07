package com.dmsBackend.response;



import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class ImportRequest {
    private MultipartFile file;
    private boolean importDatabase;
    private boolean importFiles;
    private boolean overwriteExisting;
    private String targetBasePath; // Optional: override base path
}
