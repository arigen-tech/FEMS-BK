package com.dmsBackend.controller;

import com.dmsBackend.entity.ApiAccessByRole;
import com.dmsBackend.entity.ApiEndpoint;
import com.dmsBackend.entity.ApiEndpointType;
import com.dmsBackend.response.SaveRequest;
import com.dmsBackend.service.ApiAccessByRoleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ApiAccessByRoleController {

    private static final Logger log = LoggerFactory.getLogger(ApiAccessByRoleController.class);

    private final ApiAccessByRoleService service;

    public ApiAccessByRoleController(ApiAccessByRoleService service) {
        this.service = service;
    }


    @GetMapping("/role-api-access/getAll")
    public List<ApiAccessByRole> getAll() {
        log.info("API CALL → Get All Role API Access");

        List<ApiAccessByRole> result = service.getAll();

        log.info("SUCCESS → Retrieved {} role API access records", result.size());
        return result;
    }


    @GetMapping("/role-api-access/role/{roleId}")
    public List<ApiAccessByRole> getByRole(@PathVariable Integer roleId) {
        log.info("API CALL → Get Role API Access By Role | roleId={}", roleId);

        List<ApiAccessByRole> result = service.getByRole(roleId);

        log.info("SUCCESS → Retrieved {} API access records for role {}", result.size(), roleId);
        return result;
    }


    @PostMapping("/role-api-access/create")
    public ApiAccessByRole save(@RequestBody SaveRequest request) {
        log.info("API CALL → Create Role API Access | roleId={} apiId={}",
                request.getRoleId(), request.getApiId());

        ApiAccessByRole result = service.save(
                request.getRoleId(),
                request.getApiId()
        );

        log.info("SUCCESS → Created role API access | id={} roleId={} apiId={}",
                result.getId(), result.getRole().getId(), result.getApi().getId());
        return result;
    }


    @PutMapping("/role-api-access/{id}/status")
    public ApiAccessByRole changeStatus(
            @PathVariable Integer id,
            @RequestParam Boolean status
    ) {
        log.info("API CALL → Change Role API Access Status | id={} status={}", id, status);

        ApiAccessByRole result = service.changeStatus(id, status);

        log.info("SUCCESS → Updated role API access status | id={} newStatus={}", id, status);
        return result;
    }



    //================================ ApiEndpointController ============================================

    @PostMapping("/apiEndpoint/create")
    public ApiEndpoint createApiEndpoint(@RequestBody ApiEndpoint apiEndpoint) {
        log.info("API CALL → Create API Endpoint | name={} endpointType={}",
                apiEndpoint.getEndpoint(), apiEndpoint.getEndpointType().getName());

        ApiEndpoint result = service.createApiEndpoint(apiEndpoint);

        log.info("SUCCESS → Created API endpoint | id={} name={} endpointType={}",
                result.getId(), result.getEndpoint(), result.getEndpointType().getName());
        return result;
    }

    @PutMapping("/apiEndpoint/{id}")
    public ApiEndpoint updateApiEndpoint(
            @PathVariable Integer id,
            @RequestBody ApiEndpoint apiEndpoint
    ) {
        log.info("API CALL → Update API Endpoint | id={} newName={} newEndpointType={}",
                id, apiEndpoint.getEndpoint(), apiEndpoint.getEndpointType().getName());

        ApiEndpoint result = service.updateApiEndpoint(id, apiEndpoint);

        log.info("SUCCESS → Updated API endpoint | id={} name={}", id, result.getEndpoint());
        return result;
    }

    @GetMapping("/apiEndpoint/{id}")
    public ApiEndpoint getByIdApiEndpoint(@PathVariable Integer id) {
        log.info("API CALL → Get API Endpoint By ID | id={}", id);

        ApiEndpoint result = service.getByIdApiEndpoint(id);

        log.info("SUCCESS → Retrieved API endpoint | id={} name={}", id, result.getEndpoint());
        return result;
    }

    @GetMapping("/apiEndpoint/getAll")
    public List<ApiEndpoint> ApiEndpoint() {
        log.info("API CALL → Get All API Endpoints");

        List<ApiEndpoint> result = service.getAllApiEndpoint();

        log.info("SUCCESS → Retrieved {} API endpoints", result.size());
        return result;
    }


    @GetMapping("/by-type/{endpointTypeId}")
    public List<ApiEndpoint> getByEndpointType(
            @PathVariable Integer endpointTypeId) {

        log.info("API CALL → Get API Endpoints By Type | endpointTypeId={}", endpointTypeId);

        List<ApiEndpoint> result = service.getByEndpointTypeId(endpointTypeId);

        log.info("SUCCESS → Retrieved {} API endpoints for type {}", result.size(), endpointTypeId);
        return result;
    }


    //================================ ApiEndpointType Controller ============================================
    @GetMapping("/api-endpoint-types")
    public List<ApiEndpointType> getAllEndpointTypes() {
        log.info("API CALL → Get All API Endpoint Types");

        List<ApiEndpointType> result = service.getAllEndpointTypes();

        log.info("SUCCESS → Retrieved {} API endpoint types", result.size());
        return result;
    }
}