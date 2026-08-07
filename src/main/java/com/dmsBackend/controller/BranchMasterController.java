package com.dmsBackend.controller;

import com.dmsBackend.entity.BranchMaster;
import com.dmsBackend.exception.ResourceNotFoundException;
import com.dmsBackend.payloads.ApiResponse;
import com.dmsBackend.service.BranchMasterService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/branchmaster")
@Slf4j
public class BranchMasterController {

    @Autowired
    private BranchMasterService branchMasterService;

    // ======================= CREATE =======================
    @PostMapping("/save")
    public ResponseEntity<BranchMaster> createBranchMaster(@RequestBody BranchMaster branchMaster, HttpServletRequest request) {

        log.info("CONTROLLER → Create Branch API | name={}", branchMaster.getName());

        BranchMaster savebranchmaster = this.branchMasterService.saveBranchMaster(branchMaster, request);

        log.info("CONTROLLER → Branch Created Successfully | id={} name={}",
                savebranchmaster.getId(), savebranchmaster.getName());

        return new ResponseEntity<BranchMaster>(savebranchmaster, HttpStatus.CREATED);
    }

    // ======================= UPDATE =======================
    @PutMapping("update/{id}")
    public ResponseEntity<BranchMaster> updateBranch(@PathVariable Integer id, @RequestBody BranchMaster branchMaster, HttpServletRequest request) {

        log.info("CONTROLLER → Update Branch API | id={}", id);

        try {
            BranchMaster updatedBranch = branchMasterService.updateBranchMaster(branchMaster, id, request);

            log.info("CONTROLLER → Branch Updated Successfully | id={} name={}",
                    id, updatedBranch.getName());

            return new ResponseEntity<>(updatedBranch, HttpStatus.OK);
        } catch (ResourceNotFoundException e) {

            log.info("CONTROLLER → Update Failed | id={} reason=Not Found", id);

            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    // ======================= DELETE =======================
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<BranchMaster> deletebyIdBranchMaster(@PathVariable Integer id) {

        log.info("CONTROLLER → Delete Branch API | id={}", id);

        this.branchMasterService.deleteByIdBranchMaster(id);

        log.info("CONTROLLER → Branch Deleted Successfully | id={}", id);

        return new ResponseEntity(new ApiResponse("Branchmaster deleted successfully", true), HttpStatus.OK);
    }

    // ======================= FIND ALL =======================
    @GetMapping("/findAll")
    public ResponseEntity<List<BranchMaster>> findAllBranchMaster() {

        log.info("CONTROLLER → Get All Branches API");

        List<BranchMaster> allBranchMaster = this.branchMasterService.findAllBranchMaster();

        log.info("CONTROLLER → Retrieved {} Branches", allBranchMaster.size());

        return new ResponseEntity(allBranchMaster, HttpStatus.OK);
    }

    // ======================= FIND ACTIVE =======================
    @GetMapping("/findActiveRole")
    public ResponseEntity<List<BranchMaster>> findAllActiveRole() {

        log.info("CONTROLLER → Get Active Branches API");

        List<BranchMaster> allActiveBranchMaster = this.branchMasterService.findAllActiveBranchMaster(1);

        log.info("CONTROLLER → Retrieved {} Active Branches", allActiveBranchMaster.size());

        return new ResponseEntity<>(allActiveBranchMaster, HttpStatus.OK);
    }

    // ======================= FIND BY ID =======================
    @GetMapping("/findById/{id}")
    public ResponseEntity<BranchMaster> findByIdBranchMaster(@PathVariable Integer id) {

        log.info("CONTROLLER → Get Branch By ID API | id={}", id);

        Optional<BranchMaster> branchMaster = branchMasterService.findBranchMasterById(id);

        if (branchMaster.isPresent()) {
            log.info("CONTROLLER → Branch Found | id={} name={}",
                    id, branchMaster.get().getName());
            return new ResponseEntity<>(branchMaster.get(), HttpStatus.OK);
        } else {
            log.info("CONTROLLER → Branch Not Found | id={}", id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    // ======================= UPDATE STATUS =======================
    @PutMapping("updatestatus/{id}")
    public ResponseEntity<BranchMaster> updateRoleStatus(@PathVariable Integer id, @RequestBody BranchMaster branchMaster, HttpServletRequest request) {

        log.info("CONTROLLER → Update Branch Status API | id={} newStatus={}",
                id, branchMaster.getIsActive());

        try {
            BranchMaster branchMaster1 = branchMasterService.updateStatus(id, branchMaster.getIsActive(), request);

            log.info("CONTROLLER → Branch Status Updated Successfully | id={} newStatus={}",
                    id, branchMaster1.getIsActive());

            return new ResponseEntity<>(branchMaster1, HttpStatus.OK);
        } catch (ResourceNotFoundException e) {

            log.info("CONTROLLER → Status Update Failed | id={} reason=Not Found", id);

            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}