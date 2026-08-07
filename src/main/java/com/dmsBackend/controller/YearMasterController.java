package com.dmsBackend.controller;

import com.dmsBackend.entity.YearMaster;
import com.dmsBackend.exception.ResourceNotFoundException;
import com.dmsBackend.payloads.ApiResponse;
import com.dmsBackend.service.YearMasterService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/YearMaster")
@Slf4j
public class YearMasterController {

    @Autowired
    private YearMasterService yearMasterService;

    // ======================= CREATE =======================
    @PostMapping("/save")
    public ResponseEntity<YearMaster> createYearMaster(
            @RequestBody YearMaster yearMaster,
            HttpServletRequest request) {

        log.info("API CALL → Create Year | year={}", yearMaster.getName());

        YearMaster savedYear = yearMasterService.saveYearMaster(yearMaster, request);

        log.info("SUCCESS → Year Created | id={} year={}",
                savedYear.getId(), savedYear.getName());

        return new ResponseEntity<>(savedYear, HttpStatus.CREATED);
    }

    // ======================= UPDATE =======================
    @PutMapping("/update/{id}")
    public ResponseEntity<YearMaster> updateYearMaster(
            @PathVariable Integer id,
            @RequestBody YearMaster yearMaster,
            HttpServletRequest request) {

        log.info("API CALL → Update Year | id={} year={}", id, yearMaster.getName());

        try {
            YearMaster updatedYear =
                    yearMasterService.updateYearMaster(yearMaster, id, request);

            log.info("SUCCESS → Year Updated | id={} year={}",
                    updatedYear.getId(), updatedYear.getName());

            return new ResponseEntity<>(updatedYear, HttpStatus.OK);

        } catch (ResourceNotFoundException ex) {

            log.info("FAILED → Update Year | id={} reason=Not Found", id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    // ======================= DELETE =======================
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse> deleteByIdYearMaster(@PathVariable Integer id) {

        log.info("API CALL → Delete Year | id={}", id);

        yearMasterService.deleteByIdYearMaster(id);

        log.info("SUCCESS → Year Deleted | id={}", id);

        return new ResponseEntity<>(
                new ApiResponse("YearMaster deleted successfully", true),
                HttpStatus.OK
        );
    }

    // ======================= FIND ALL =======================
    @GetMapping("/findAll")
    public ResponseEntity<List<YearMaster>> findAllYearMaster() {

        log.info("API CALL → Get All Years");

        return new ResponseEntity<>(
                yearMasterService.findAllYearMaster(),
                HttpStatus.OK
        );
    }

    // ======================= FIND ACTIVE =======================
    @GetMapping("/findActiveYear")
    public ResponseEntity<List<YearMaster>> findAllActiveYearMaster() {

        log.info("API CALL → Get Active Years");

        return ResponseEntity.ok(
                yearMasterService.findAllActiveYearMaster(1)
        );
    }

    // ======================= FIND BY ID =======================
    @GetMapping("/findById/{id}")
    public ResponseEntity<YearMaster> findByIdYearMaster(@PathVariable Integer id) {

        log.info("API CALL → Get Year By ID | id={}", id);

        Optional<YearMaster> yearMaster =
                yearMasterService.findYearMasterById(id);

        return yearMaster
                .map(value -> {
                    log.info("SUCCESS → Year Found | id={} year={}",
                            value.getId(), value.getName());
                    return new ResponseEntity<>(value, HttpStatus.OK);
                })
                .orElseGet(() -> {
                    log.info("FAILED → Get Year | id={} reason=Not Found", id);
                    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                });
    }

    // ======================= STATUS UPDATE =======================
    @PutMapping("/updatestatus/{id}")
    public ResponseEntity<YearMaster> updateYearStatus(
            @PathVariable Integer id,
            @RequestBody YearMaster yearMaster,
            HttpServletRequest request) {

        log.info("API CALL → Update Year Status | id={} newStatus={}",
                id, yearMaster.getIsActive());

        try {
            YearMaster updated =
                    yearMasterService.updateStatus(id, yearMaster.getIsActive(), request);

            log.info("SUCCESS → Year Status Updated | id={} status={}",
                    updated.getId(), updated.getIsActive());

            return new ResponseEntity<>(updated, HttpStatus.OK);

        } catch (ResourceNotFoundException ex) {

            log.info("FAILED → Status Update | id={} reason=Not Found", id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
