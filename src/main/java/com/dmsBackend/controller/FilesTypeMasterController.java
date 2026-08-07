package com.dmsBackend.controller;

import com.dmsBackend.entity.FilesTypeMaster;
import com.dmsBackend.response.ApiResponse;
import com.dmsBackend.service.FilesTypeMasterService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/file-type")
@Slf4j
public class FilesTypeMasterController {

    @Autowired
    private FilesTypeMasterService filesTypeMasterService;

    // ======================= CREATE =======================
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<FilesTypeMaster>> createFilesTypeMaster(
            @RequestBody FilesTypeMaster filesTypeMaster,
            HttpServletRequest request) {

        log.info("API CALL → Create File Type | name={}", filesTypeMaster.getFiletype());

        ApiResponse<FilesTypeMaster> response =
                filesTypeMasterService.createFilesTypeMaster(filesTypeMaster, request);

        log.info("API RESPONSE → Create File Type | status={}", response.getStatus());

        return ResponseEntity
                .status(response.getStatus() != 0 ? response.getStatus() : HttpStatus.OK.value())
                .body(response);
    }

    // ======================= GET ALL =======================
    @GetMapping("/getAll")
    public ResponseEntity<ApiResponse<List<FilesTypeMaster>>> getAllFilesTypeMaster() {

        log.info("API CALL → Get All File Types");

        return ResponseEntity.ok(filesTypeMasterService.getAllFilesTypeMaster());
    }

    // ======================= GET ALL ACTIVE =======================
    @GetMapping("/getAllActive")
    public ResponseEntity<ApiResponse<List<FilesTypeMaster>>> getAllActiveFilesTypeMaster() {

        log.info("API CALL → Get All Active File Types");

        return ResponseEntity.ok(filesTypeMasterService.getAllActiveFilesTypeMaster());
    }

    // ======================= GET BY ID =======================
    @GetMapping("/getById/{id}")
    public ResponseEntity<ApiResponse<FilesTypeMaster>> getFilesTypeMasterById(
            @PathVariable Integer id) {

        log.info("API CALL → Get File Type By ID | id={}", id);

        return ResponseEntity.ok(filesTypeMasterService.getFilesTypeMasterById(id));
    }

    // ======================= UPDATE =======================
    @PutMapping("/updateById/{id}")
    public ResponseEntity<ApiResponse<FilesTypeMaster>> updateFilesTypeMaster(
            @PathVariable Integer id,
            @RequestBody FilesTypeMaster filesTypeMaster,
            HttpServletRequest request) {

        log.info("API CALL → Update File Type | id={} name={}",
                id, filesTypeMaster.getFiletype());

        return ResponseEntity.ok(
                filesTypeMasterService.updateFilesTypeMaster(id, filesTypeMaster, request)
        );
    }

    // ======================= STATUS UPDATE =======================
    @PutMapping("/update/status/{id}")
    public ResponseEntity<ApiResponse<FilesTypeMaster>> updateFilesTypeMasterStatus(
            @PathVariable Integer id,
            @RequestParam Integer status,
            HttpServletRequest request) {

        log.info("API CALL → Update File Type Status | id={} status={}", id, status);

        return ResponseEntity.ok(
                filesTypeMasterService.updateFileTypeStatus(id, status, request)
        );
    }
}
