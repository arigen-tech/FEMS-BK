package com.dmsBackend.controller;

import com.dmsBackend.entity.DepartmentMaster;
import com.dmsBackend.exception.ResourceNotFoundException;
import com.dmsBackend.payloads.ApiResponse;
import com.dmsBackend.response.DepartmentResponse;
import com.dmsBackend.service.DepartmentMasterService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/DepartmentMaster")
@Slf4j
public class DepartmentMasterController {

    @Autowired
    DepartmentMasterService departmentMasterService;

    // ======================= CREATE =======================
    @PostMapping("/save")
    public ResponseEntity<DepartmentMaster> createDepartmentMaster(@RequestBody DepartmentMaster departmentMaster, HttpServletRequest request) {

        log.info("CONTROLLER → Create Department API | name={}", departmentMaster.getName());

        DepartmentMaster saveDepartmentMaster = this.departmentMasterService.saveDepartmentMaster(departmentMaster, request);

        log.info("CONTROLLER → Department Created Successfully | id={} name={}",
                saveDepartmentMaster.getId(), saveDepartmentMaster.getName());

        return new ResponseEntity<DepartmentMaster>(saveDepartmentMaster, HttpStatus.CREATED);
    }

    // ======================= UPDATE =======================
    @PutMapping("update/{id}")
    public ResponseEntity<DepartmentMaster> updateDepartmentMaster(@PathVariable Integer id, @RequestBody DepartmentMaster departmentMaster, HttpServletRequest request) {

        log.info("CONTROLLER → Update Department API | id={}", id);

        try {
            DepartmentMaster updateddepartmentMaster = departmentMasterService.updateDepartmentMaster(departmentMaster, id, request);

            log.info("CONTROLLER → Department Updated Successfully | id={} name={}",
                    id, updateddepartmentMaster.getName());

            return new ResponseEntity<>(updateddepartmentMaster, HttpStatus.OK);
        } catch (ResourceNotFoundException e) {

            log.info("CONTROLLER → Update Failed | id={} reason=Not Found", id);

            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    // ======================= DELETE =======================
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<DepartmentMaster> deletebyIdDepartmentMaster(@PathVariable Integer id) {

        log.info("CONTROLLER → Delete Department API | id={}", id);

        this.departmentMasterService.deleteByIdDepartmentMaster(id);

        log.info("CONTROLLER → Department Deleted Successfully | id={}", id);

        return new ResponseEntity(new ApiResponse("DepartmentMaster deleted successfully", true), HttpStatus.OK);
    }

    // ======================= FIND ALL =======================
    @GetMapping("/findAll")
    public ResponseEntity<List<DepartmentResponse>> findAllBranchMaster() {

        log.info("CONTROLLER → Get All Departments API");

        List<DepartmentResponse> allDepartmentMasterMaster = this.departmentMasterService.findAllDepartmentMaster();

        log.info("CONTROLLER → Retrieved {} Departments", allDepartmentMasterMaster.size());

        return new ResponseEntity(allDepartmentMasterMaster, HttpStatus.OK);
    }

    // ======================= FIND ACTIVE =======================
    @GetMapping("/findActiveRole")
    public ResponseEntity<List<DepartmentMaster>> findAllActiveRole() {

        log.info("CONTROLLER → Get Active Departments API");

        List<DepartmentMaster> allActiveDeptMaster = this.departmentMasterService.findAllActiveDepartmentMaster(1);

        log.info("CONTROLLER → Retrieved {} Active Departments", allActiveDeptMaster.size());

        return new ResponseEntity<>(allActiveDeptMaster, HttpStatus.OK);
    }

    // ======================= FIND BY ID =======================
    @GetMapping("/findById/{id}")
    public ResponseEntity<DepartmentMaster> findByIdDepartmentMaster(@PathVariable Integer id) {

        log.info("CONTROLLER → Get Department By ID API | id={}", id);

        Optional<DepartmentMaster> departmentMasterMaster = departmentMasterService.findDepartmentMasterById(id);

        if (departmentMasterMaster.isPresent()) {
            log.info("CONTROLLER → Department Found | id={} name={}",
                    id, departmentMasterMaster.get().getName());
            return new ResponseEntity<>(departmentMasterMaster.get(), HttpStatus.OK);
        } else {
            log.info("CONTROLLER → Department Not Found | id={}", id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    // ======================= UPDATE STATUS =======================
    @PutMapping("/updateDeptStatus/{id}")
    public ResponseEntity<DepartmentMaster> updateStatusDepartment(
            @PathVariable Integer id,
            @RequestBody Integer isActive, HttpServletRequest request) {

        log.info("CONTROLLER → Update Department Status API | id={} newStatus={}", id, isActive);

        try {
            DepartmentMaster departmentMaster = this.departmentMasterService.updateStatusDepartment(id, isActive, request);

            log.info("CONTROLLER → Department Status Updated Successfully | id={} newStatus={}",
                    id, departmentMaster.getIsActive());

            return new ResponseEntity<>(departmentMaster, HttpStatus.OK);
        } catch (ResourceNotFoundException e) {

            log.info("CONTROLLER → Status Update Failed | id={} reason=Not Found", id);

            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {

            log.info("CONTROLLER → Status Update Failed | id={} reason={}", id, e.getMessage());

            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ======================= FIND BY BRANCH =======================
    @GetMapping("/findByBranch/{branchId}")
    public List<DepartmentMaster> findByBranch(@PathVariable Integer branchId) {

        log.info("CONTROLLER → Get Departments By Branch API | branchId={}", branchId);

        List<DepartmentMaster> departments = departmentMasterService.findDepartmentMasterByBranch(branchId);

        log.info("CONTROLLER → Retrieved {} Departments for Branch ID: {}", departments.size(), branchId);

        return departments;
    }
}