package com.dmsBackend.controller;

import com.dmsBackend.entity.MasterType;
import com.dmsBackend.response.MasterRequest;
import com.dmsBackend.service.MasterService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * ONE controller for ALL master/lookup types added for Register Case
 * & Evidence: Case Type, Crime Type, State, District, City, Priority,
 * Evidence Type, Forwarding Authority Type, Mode of Submission,
 * Package Type.
 *
 * {type} in the URL is the MasterType key, e.g.:
 *   GET  /master/case-type/getAll/1
 *   GET  /master/district/getByParent/{stateId}/1     -> districts for a state
 *   GET  /master/city/getByParent/{districtId}/1      -> cities for a district
 *   GET  /master/evidence-type/getByParent/{categoryId}/1
 *   POST /master/case-type/create
 *   PUT  /master/case-type/update/{id}
 *   PUT  /master/case-type/status/{id}?isActive=true
 *
 * Adding an 11th master later needs zero changes here — just a new
 * Entity + Repository + ServiceImpl + MasterType entry.
 */
@RestController
@RequestMapping(value = "/master", produces = MediaType.APPLICATION_JSON_VALUE)
@CrossOrigin
@Slf4j
public class MasterController {

    @Autowired
    private Map<String, MasterService> masterServiceMap;

    private MasterService<?> resolveService(String type) {
        MasterType masterType = MasterType.fromKey(type);
        MasterService<?> service = masterServiceMap.get(masterType.getBeanName());
        if (service == null) {
            throw new IllegalStateException("No service registered for master type: " + type);
        }
        return service;
    }

    // ================= GET ALL =================
    @GetMapping("/{type}/getAll/{flag}")
    public ResponseEntity<?> getAll(@PathVariable String type, @PathVariable int flag) {
        log.info("Fetching all {} records with flag: {}", type, flag);
        return ResponseEntity.ok(resolveService(type).findAll(flag));
    }

    // ================= GET BY PARENT (cascading dropdowns) =================
    @GetMapping("/{type}/getByParent/{parentId}/{flag}")
    public ResponseEntity<?> getByParent(@PathVariable String type,
                                         @PathVariable Integer parentId,
                                         @PathVariable int flag) {
        log.info("Fetching {} records for parentId: {} flag: {}", type, parentId, flag);
        return ResponseEntity.ok(resolveService(type).findByParent(parentId, flag));
    }

    // ================= GET BY ID =================
    @GetMapping("/{type}/getById/{id}")
    public ResponseEntity<?> getById(@PathVariable String type, @PathVariable Integer id) {
        log.info("Fetching {} by id: {}", type, id);
        return ResponseEntity.ok(resolveService(type).findById(id));
    }

    // ================= CREATE =================
    @PostMapping("/{type}/create")
    public ResponseEntity<?> create(@PathVariable String type,
                                    @RequestBody MasterRequest request,
                                    HttpServletRequest httpRequest) {
        log.info("Creating {} with request: {}", type, request);
        Object saved = resolveService(type).save(request, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // ================= UPDATE =================
    @PutMapping("/{type}/update/{id}")
    public ResponseEntity<?> update(@PathVariable String type,
                                    @PathVariable Integer id,
                                    @RequestBody MasterRequest request,
                                    HttpServletRequest httpRequest) {
        log.info("Updating {} with id: {}", type, id);
        return ResponseEntity.ok(resolveService(type).update(request, id, httpRequest));
    }

    // ================= STATUS UPDATE =================
    @PutMapping("/{type}/status/{id}")
    public ResponseEntity<?> updateStatus(@PathVariable String type,
                                          @PathVariable Integer id,
                                          @RequestParam Boolean isActive,
                                          HttpServletRequest request) {
        log.info("Updating status for {} id: {} to {}", type, id, isActive);
        return ResponseEntity.ok(resolveService(type).updateStatus(id, isActive, request));
    }
}
