package com.dmsBackend.controller;

import com.dmsBackend.entity.LanguageMaster;
import com.dmsBackend.response.LanguageMasterRequest;
import com.dmsBackend.service.LanguageMasterService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/languageMaster")
@CrossOrigin
@Slf4j
public class LanguageMasterController {

    @Autowired
    private LanguageMasterService languageMasterService;

    // ================= GET ALL =================
    @GetMapping("/getAll/{flag}")
    public ResponseEntity<List<LanguageMaster>> getAllLanguageMaster(
            @PathVariable int flag) {

        log.info("Fetching all LanguageMaster records with flag: {}", flag);

        List<LanguageMaster> list =
                languageMasterService.findAllLanguageMaster(flag);

        log.info("Fetched {} LanguageMaster records", list.size());
        return ResponseEntity.ok(list);
    }

    // ================= GET BY ID =================
    @GetMapping("/getById/{id}")
    public ResponseEntity<LanguageMaster> getLanguageMasterById(
            @PathVariable Long id) {

        log.info("Fetching LanguageMaster by id: {}", id);

        LanguageMaster languageMaster =
                languageMasterService.findLanguageMasterById(id);

        log.info("Fetched LanguageMaster with id: {}", id);
        return ResponseEntity.ok(languageMaster);
    }

    // ================= CREATE =================
    @PostMapping("/create")
    public ResponseEntity<LanguageMaster> createLanguageMaster(
            @RequestBody LanguageMasterRequest request,
            HttpServletRequest httpRequest) {

        log.info("Creating LanguageMaster with request: {}", request);

        LanguageMaster saved =
                languageMasterService.saveLanguageMaster(request, httpRequest);

        log.info("LanguageMaster created successfully with id: {}", saved.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // ================= UPDATE =================
    @PutMapping("/update/{id}")
    public ResponseEntity<LanguageMaster> updateLanguageMaster(
            @PathVariable Long id,
            @RequestBody LanguageMasterRequest request,
            HttpServletRequest httpRequest) {

        log.info("Updating LanguageMaster with id: {}", id);

        LanguageMaster updated =
                languageMasterService.updateLanguageMaster(request, id, httpRequest);

        log.info("LanguageMaster updated successfully with id: {}", id);
        return ResponseEntity.ok(updated);
    }

    // ================= STATUS UPDATE =================
    @PutMapping("/status/{id}")
    public ResponseEntity<LanguageMaster> updateLanguageStatus(
            @PathVariable Long id,
            @RequestParam Boolean isActive,
            HttpServletRequest request) {

        log.info("Updating status for LanguageMaster id: {} to {}", id, isActive);

        LanguageMaster updated =
                languageMasterService.updateStatus(id, isActive, request);

        log.info("Status updated successfully for LanguageMaster id: {}", id);
        return ResponseEntity.ok(updated);
    }
}
