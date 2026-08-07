package com.dmsBackend.controller;

import com.dmsBackend.service.UploadFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/uploadFile")
//@CrossOrigin("https://happytyagi.github.io/DmsFrontend/")
public class UploadFileController {

    @Value("${file.upload-dir:images/}")  // Use property for path, with a default value
    private String path;

    @Autowired
    private UploadFileService uploadFileService;

    @PostMapping("/File")
    public ResponseEntity<String> uploadFile(
            @RequestParam("images") MultipartFile image,
            @RequestParam("category") String category) throws IOException {

        // Call the service method with the category
        String fileName = uploadFileService.uploadImage(path, image, category);

        return new ResponseEntity<>(fileName, HttpStatus.OK);
    }
}
