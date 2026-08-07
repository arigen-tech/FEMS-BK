package com.dmsBackend.service;


import com.dmsBackend.entity.FilesTypeMaster;
import com.dmsBackend.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public interface FilesTypeMasterService {
    ApiResponse<List<FilesTypeMaster>> getAllFilesTypeMaster();

    ApiResponse<List<FilesTypeMaster>> getAllActiveFilesTypeMaster();

    ApiResponse<FilesTypeMaster> getFilesTypeMasterById(Integer id);

    @Transactional(rollbackFor = {Exception.class})
    ApiResponse<FilesTypeMaster> createFilesTypeMaster(FilesTypeMaster filesTypeMaster, HttpServletRequest request);

    @Transactional(rollbackFor = {Exception.class})
    ApiResponse<FilesTypeMaster> updateFilesTypeMaster(Integer id, FilesTypeMaster updatedFilesTypeMaster, HttpServletRequest request);

    @Transactional(rollbackFor = {Exception.class})
    ApiResponse<FilesTypeMaster> updateFileTypeStatus(Integer fileTypeId, Integer status, HttpServletRequest request);
}
