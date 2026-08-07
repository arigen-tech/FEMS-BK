package com.dmsBackend.controller;

import com.dmsBackend.entity.RoleMaster;
import com.dmsBackend.exception.ResourceNotFoundException;
import com.dmsBackend.payloads.ApiResponse;
import com.dmsBackend.service.RoleMasterService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/RoleMaster")
@Slf4j
public class RoleMasterController {

    @Autowired
    private RoleMasterService roleMasterService;

    // ======================= CREATE =======================
    @PostMapping("/save")
    public ResponseEntity<RoleMaster> createRoleMaster(
            @RequestBody RoleMaster roleMaster,
            HttpServletRequest request) {

        log.info("API CALL → Create Role | name={}", roleMaster.getRole());

        RoleMaster savedRole =
                roleMasterService.saveRoleMaster(roleMaster, request);

        log.info("SUCCESS → Role Created | id={} name={}",
                savedRole.getId(), savedRole.getRole());

        return new ResponseEntity<>(savedRole, HttpStatus.CREATED);
    }

    // ======================= UPDATE =======================
    @PutMapping("/update/{id}")
    public ResponseEntity<RoleMaster> updateRoleMaster(
            @PathVariable Integer id,
            @RequestBody RoleMaster roleMaster,
            HttpServletRequest request) {

        log.info("API CALL → Update Role | id={} name={}",
                id, roleMaster.getRole());

        try {
            RoleMaster updatedRole =
                    roleMasterService.updateRoleMaster(roleMaster, id, request);

            log.info("SUCCESS → Role Updated | id={} name={}",
                    updatedRole.getId(), updatedRole.getRole());

            return new ResponseEntity<>(updatedRole, HttpStatus.OK);

        } catch (ResourceNotFoundException ex) {

            log.info("FAILED → Update Role | id={} reason=Not Found", id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    // ======================= DELETE =======================
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse> deleteByIdRoleMaster(@PathVariable Integer id) {

        log.info("API CALL → Delete Role | id={}", id);

        roleMasterService.deleteByIdRoleMaster(id);

        log.info("SUCCESS → Role Deleted | id={}", id);

        return new ResponseEntity<>(
                new ApiResponse("RoleMaster deleted successfully", true),
                HttpStatus.OK
        );
    }

    // ======================= FIND ALL =======================
    @GetMapping("/findAll")
    public ResponseEntity<List<RoleMaster>> findAllRoleMaster() {

        log.info("API CALL → Get All Roles");

        return new ResponseEntity<>(
                roleMasterService.findAllRoleMaster(),
                HttpStatus.OK
        );
    }

    // ======================= FIND ACTIVE =======================
    @GetMapping("/findActiveRole")
    public ResponseEntity<List<RoleMaster>> findAllActiveRole() {

        log.info("API CALL → Get Active Roles");

        return new ResponseEntity<>(
                roleMasterService.findAllActiveRoleMaster(true),
                HttpStatus.OK
        );
    }

    // ======================= FIND BY ID =======================
    @GetMapping("/findById/{id}")
    public ResponseEntity<RoleMaster> findByIdRoleMaster(
            @PathVariable Integer id) {

        log.info("API CALL → Get Role By ID | id={}", id);

        Optional<RoleMaster> roleMaster =
                roleMasterService.findRoleMasterById(id);

        return roleMaster
                .map(value -> {
                    log.info("SUCCESS → Role Found | id={} name={}",
                            value.getId(), value.getRole());
                    return new ResponseEntity<>(value, HttpStatus.OK);
                })
                .orElseGet(() -> {
                    log.info("FAILED → Get Role | id={} reason=Not Found", id);
                    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                });
    }

    // ======================= STATUS UPDATE =======================
    @PutMapping("/updatestatus/{id}")
    public ResponseEntity<RoleMaster> updateRoleStatus(
            @PathVariable Integer id,
            @RequestBody RoleMaster roleMaster,
            HttpServletRequest request) {

        log.info("API CALL → Update Role Status | id={} newStatus={}",
                id, roleMaster.getIsActive());

        try {
            RoleMaster updatedRole =
                    roleMasterService.updateStatus(id, roleMaster.getIsActive(), request);

            log.info("SUCCESS → Role Status Updated | id={} status={}",
                    updatedRole.getId(), updatedRole.getIsActive());

            return new ResponseEntity<>(updatedRole, HttpStatus.OK);

        } catch (ResourceNotFoundException ex) {

            log.info("FAILED → Status Update | id={} reason=Not Found", id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
