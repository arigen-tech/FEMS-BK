package com.dmsBackend.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface UploadFileService {
    String uploadImage(String path, MultipartFile image, String category) throws IOException;
}
