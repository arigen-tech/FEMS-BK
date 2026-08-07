package com.dmsBackend.controller;
import com.dmsBackend.response.*;
import com.dmsBackend.response.ApiResponse;
import com.dmsBackend.service.DynamicSidebarService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@Tag(name = "Dynamic Sidebar Controller", description = "Controller for handling Dynamic Sidebar")
@RequestMapping("/dynamic-sidebar")
@RequiredArgsConstructor
public class DynamicSidebarController {

    private final DynamicSidebarService dynamicSidebarService;

    //-------------------------------------------UserApplication------------------------------------------------------//

    @PostMapping("/application/create")
    public ResponseEntity<?> createApplication(@RequestBody UserApplicationRequest request){
        return  ResponseEntity.status(HttpStatus.CREATED).body(dynamicSidebarService.createApplication(request));
    }

    @PutMapping("/application/edit/{id}")
    public ResponseEntity<?> updateApplication(@PathVariable Long id, @RequestBody UserApplicationRequest request){
        return  ResponseEntity.ok(dynamicSidebarService.updateApplicationById(id,request));
    }

    @PutMapping("/application/status/{id}")
    public ResponseEntity<?> changeApplicationStatus(@PathVariable Long id,@RequestParam String status){
        return  ResponseEntity.ok(dynamicSidebarService.changeStatusById(id,status));
    }

    @GetMapping("/application/{id}")
    public ResponseEntity<?> getApplicationById(@PathVariable Long id){
        return  ResponseEntity.ok(dynamicSidebarService.getApplicationById(id));
    }

    @GetMapping("/application/getAllUserApplications/{flag}")
    public ResponseEntity<?> getApplicationById(@PathVariable int flag){
        return  ResponseEntity.ok(dynamicSidebarService.getAllApplications(flag));
    }

    @GetMapping("/application/getAllParentId/{flag}")
    public ResponseEntity<?> getAllApplicationsWithHashUrl(@PathVariable int flag) {
        return ResponseEntity.ok(dynamicSidebarService.getAllApplicationsWithHashUrl(flag));
    }


    //-------------------------------------------MasApplication------------------------------------------------------//


    @GetMapping("/mas-applications/getAll/{flag}")
    public ApiResponse<List<MasApplicationResponse>> getAllApplications(@PathVariable int flag) {//ApiResponse<List<MasApplicationResponse>>
        return dynamicSidebarService.getAllMasApplications(flag);
    }

    @GetMapping("/mas-applications/getById{id}")
    public ResponseEntity<ApiResponse<MasApplicationResponse>> getApplicationById(@PathVariable String id) {
        return ResponseEntity.ok(dynamicSidebarService.getApplicationById(id));
    }

    @PostMapping("/mas-applications/create")
    public ResponseEntity<ApiResponse<MasApplicationResponse>> createApplication(@RequestBody MasApplicationRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(dynamicSidebarService.createApplication(request));
    }

    @PutMapping("/mas-applications/UpdateById/{id}")
    public ResponseEntity<ApiResponse<MasApplicationResponse>> updateApplication(@PathVariable String id, @RequestBody MasApplicationRequest request) {
        return ResponseEntity.ok(dynamicSidebarService.updateApplication(id, request));
    }

    @GetMapping("/mas-applications/getAllChildrenByParentId/{parentId}")
    public ResponseEntity<ApiResponse<List<MasApplicationResponse>>> getAllByParentId(@PathVariable String parentId, @RequestParam(required = false) Long templateId) {

        return new ResponseEntity<>(
                dynamicSidebarService.getAllByParentId(parentId, templateId), org.springframework.http.HttpStatus.OK
        );
    }

    @PutMapping("/mas-applications/updateBatchStatus")
    public ResponseEntity<ApiResponse<String>> updateMultipleApplicationStatuses(@RequestBody UpdateStatusRequest request) {
        return new ResponseEntity<>(dynamicSidebarService.updateMultipleApplicationStatuses(request), HttpStatus.OK);
    }

    @GetMapping("/mas-applications/getAllParents/{flag}")
    public ResponseEntity<ApiResponse<List<MasApplicationResponse>>> getAllParentApplications(@PathVariable int flag) {
        return new ResponseEntity<>(dynamicSidebarService.getAllParentApplications(flag), HttpStatus.OK);
    }

    @PostMapping("/mas-applications/assignUpdateTemplate")
    public ResponseEntity<ApiResponse<String>> processBatchUpdates(@RequestBody BatchUpdateRequest request) {
        ApiResponse<String> response = dynamicSidebarService.processBatchUpdates(request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }


    //================================================MasTemplate========================================================//



    @GetMapping("/mas-templates/getAll/{flag}")
    public ApiResponse<List<MasTemplateResponse>> getAllTemplates(@PathVariable int flag) {
        return dynamicSidebarService.getAllTemplates(flag);
    }

    @GetMapping("/mas-templates/getById/{id}")
    public ResponseEntity<ApiResponse<MasTemplateResponse>> getTemplateById(@PathVariable Long id) {
        return new ResponseEntity<>(dynamicSidebarService.getTemplateById(id), HttpStatus.OK);
    }

    @PostMapping("/mas-templates/create")
    public ResponseEntity<ApiResponse<MasTemplateResponse>> createTemplate(@RequestBody MasTemplateRequest request) {
        return new ResponseEntity<>(dynamicSidebarService.createTemplate(request), HttpStatus.CREATED);
    }

    @PutMapping("/mas-templates/updateById/{id}")
    public ResponseEntity<ApiResponse<MasTemplateResponse>> updateTemplate(@PathVariable Long id, @RequestBody MasTemplateRequest request) {
        return new ResponseEntity<>(dynamicSidebarService.updateTemplate(id, request), HttpStatus.OK);
    }

    @PutMapping("/mas-templates/status/{id}")
    public ResponseEntity<ApiResponse<String>> changeTemplateStatus(@PathVariable Long id, @RequestParam String status) {
        return new ResponseEntity<>(dynamicSidebarService.changeTemplateStatus(id, status), HttpStatus.OK);
    }




    //==========================================================RoleTemplate===========================================================//



    @PostMapping("/role-template/assignTemplates")
    public ApiResponse<List<RoleTemplateResponse>> addOrUpdateRoleTemplates(@RequestBody RoleTemplateRequestList requestList) {
        return dynamicSidebarService.addOrUpdateRoleTemplates(requestList);
    }

    @GetMapping("/role-template/getAllAssignedTemplates/{roleId}/{flag}")
    public ApiResponse<List<RoleTemplateResponse>> getTemplatesByRoleId(
            @PathVariable Long roleId,
            @PathVariable int flag) {
        return dynamicSidebarService.getTemplatesByRoleId(roleId, flag);
    }



    //==========================================================TemplateApplication====================================================//



    @PostMapping("/template-applications/assignAppTemplate")
    public ResponseEntity<ApiResponse<TemplateApplicationResponse>> assignTemplateToApplication(@RequestBody TemplateApplicationRequest request) {
        ApiResponse<TemplateApplicationResponse> response = dynamicSidebarService.assignTemplateToApplication(request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping("/template-applications/changeStatus")
    public ResponseEntity<ApiResponse<String>> changeTemplateApplicationStatus(
            @RequestParam Long id,
            @RequestParam String status) {
        ApiResponse<String> response = dynamicSidebarService.changeTemplateApplicationStatus(id, status);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/template-applications/getAllTemplateById/{templateId}")
    public ResponseEntity<ApiResponse<List<TemplateApplicationResponse>>> getAllTemplateById(@PathVariable Long templateId) {
        return ResponseEntity.ok(dynamicSidebarService.getAllTemplateById(templateId));
    }

    @GetMapping("/template-applications/getAllTemplateApplications/{flag}")
    public ApiResponse<List<TemplateApplicationResponse>> getAllTemplateApplications(@PathVariable int flag) {
        return dynamicSidebarService.getAllTemplateApplications(flag);
    }



    //============================================get final==============================================

    @GetMapping("/getAllUrlByRoles/{roleIds}")
    public ApiResponse getTemplatesByRoleIds(@PathVariable String roleIds) {
        List<Long> roleIdList = Arrays.stream(roleIds.split(","))
                .map(String::trim)
                .map(Long::parseLong)
                .collect(Collectors.toList());

        return dynamicSidebarService.getAllUrlByRoleIds(roleIdList);
    }

}
