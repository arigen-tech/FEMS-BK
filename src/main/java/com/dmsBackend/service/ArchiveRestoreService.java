package com.dmsBackend.service;

import com.dmsBackend.response.ArchiveRestoreDTO;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public interface ArchiveRestoreService {
    void restoreArchive(ArchiveRestoreDTO restoreDTO, HttpServletResponse response) throws IOException;
}

