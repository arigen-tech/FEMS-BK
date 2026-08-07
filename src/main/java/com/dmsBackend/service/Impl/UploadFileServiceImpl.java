package com.dmsBackend.service.Impl;

import com.dmsBackend.service.UploadFileService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class UploadFileServiceImpl implements UploadFileService {

    @Override
    public String uploadImage(String uploadPath, MultipartFile file, String category) throws IOException {
        // Create the upload directory if it doesn't exist
        File directory = new File(uploadPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // Create a unique file name
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(uploadPath + fileName);

        // Copy the file to the destination
        Files.copy(file.getInputStream(), filePath);

        return fileName; // Return the file name or full path if needed
    }
}
