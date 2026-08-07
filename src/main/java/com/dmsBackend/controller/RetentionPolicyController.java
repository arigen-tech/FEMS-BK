package com.dmsBackend.controller;

import com.dmsBackend.P5Archive.P5DashboardRes1;
import com.dmsBackend.entity.*;
import com.dmsBackend.response.NewRetentionPolicyRequest;
import com.dmsBackend.response.RetentionPolicyDTO;
import com.dmsBackend.service.RetentionPolicyService;
import com.dmsBackend.utils.ResponseUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@RestController
@RequestMapping("/retention-policy")
@Slf4j
public class RetentionPolicyController {

    private final RetentionPolicyService retentionPolicyService;


    public RetentionPolicyController(RetentionPolicyService retentionPolicyService
    ) {
        this.retentionPolicyService = retentionPolicyService;

    }


    @PostMapping("/createNew")
    public ResponseEntity<?> createNewPolicy(@RequestBody NewRetentionPolicyRequest newRequest) {
        RetentionPolicy saved = retentionPolicyService.NewCreatePolicy(newRequest);
        RetentionPolicyDTO response = toResponse(saved);
        return ResponseEntity.ok(ResponseUtils.createSuccessResponse(response, new TypeReference<>() {}));
    }

    @PutMapping("/updateNewPolicy/{id}")
    public ResponseEntity<RetentionPolicy> updatePolicy(
            @PathVariable Long id,
            @RequestBody NewRetentionPolicyRequest req) {
        RetentionPolicy updatedPolicy = retentionPolicyService.updateNewPolicy(id, req);
        return ResponseEntity.ok(updatedPolicy);
    }


    @GetMapping("/findAll")
    public ResponseEntity<?> getAllPolicies() {
        List<RetentionPolicy> policies = retentionPolicyService.findAll();
        List<RetentionPolicyDTO> responseList = policies.stream().map(this::toResponse).toList();
        return ResponseEntity.ok(ResponseUtils.createSuccessResponse(responseList, new TypeReference<>() {}));
    }

    @GetMapping("/findAllByFilter")
    public List<P5DashboardRes1> getPolicies(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long departmentId) {

        return retentionPolicyService.findAll(branchId, departmentId);
    }



    private RetentionPolicyDTO toResponse(RetentionPolicy policy) {
        RetentionPolicyDTO res = new RetentionPolicyDTO();
        res.setId(policy.getId());
        res.setDescription(policy.getDescription());
        res.setIsActive(policy.getIsActive());
        res.setPolicyType(policy.getPolicyType());
        res.setRetentionDate(policy.getRetentionDate());
        res.setRetentionTime(policy.getRetentionTime());
        res.setFromdate(policy.getFromDate());
        res.setTodate(policy.getToDate());
        res.setCreatedOn(policy.getCreatedOn());
        res.setUpdatedOn(policy.getUpdatedOn());

        res.setBranchId(policy.getBranch() != null ? policy.getBranch().getId() : null);
        res.setDepartmentId(policy.getDepartment() != null ? policy.getDepartment().getId() : null);
        res.setCategoryId(policy.getCategory() != null ? policy.getCategory().getId() : null);

        // Add names for display purposes
//        res.setBranchName(policy.getBranch() != null ? policy.getBranch().getName() : null);
//        res.setDepartmentName(policy.getDepartment() != null ? policy.getDepartment().getName() : null);

        return res;
    }



}