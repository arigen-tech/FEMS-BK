package com.dmsBackend.service.Impl;

import com.dmsBackend.entity.ApiAccessByRole;
import com.dmsBackend.entity.ApiEndpoint;
import com.dmsBackend.entity.ApiEndpointType;
import com.dmsBackend.entity.RoleMaster;
import com.dmsBackend.repository.ApiAccessByRoleRepository;
import com.dmsBackend.repository.ApiEndpointRepository;
import com.dmsBackend.repository.ApiEndpointTypeRepository;
import com.dmsBackend.repository.RoleMasterRepository;
import com.dmsBackend.service.ApiAccessByRoleService;
import com.dmsBackend.utils.CurrentUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;

@Service
public class ApiAccessByRoleServiceImpl implements ApiAccessByRoleService {

    private static final Logger log = LoggerFactory.getLogger(ApiAccessByRoleServiceImpl.class);

    private final ApiAccessByRoleRepository repository;
    private final RoleMasterRepository roleRepo;
    private final ApiEndpointRepository apiRepo;

    private final ApiEndpointTypeRepository apiEndpointTypeRepository;

    private final CurrentUser currentUser;
    private final ApiEndpointRepository apiEndpointRepository;

    public ApiAccessByRoleServiceImpl(
            ApiAccessByRoleRepository repository,
            RoleMasterRepository roleRepo,
            ApiEndpointRepository apiRepo,
            ApiEndpointRepository apiEndpointRepository,
            CurrentUser currentUser,
            ApiEndpointTypeRepository apiEndpointTypeRepository
    ) {
        this.repository = repository;
        this.roleRepo = roleRepo;
        this.apiRepo = apiRepo;
        this.apiEndpointRepository = apiEndpointRepository;
        this.currentUser = currentUser;
        this.apiEndpointTypeRepository = apiEndpointTypeRepository;
    }

    @Override
    public List<ApiAccessByRole> getAll() {
        log.info("API CALL → Get All Role API Access");

        List<ApiAccessByRole> result = repository.findAll();

        log.info("SUCCESS → Retrieved {} role API access records", result.size());
        return result;
    }

    @Override
    public List<ApiAccessByRole> getByRole(Integer roleId) {
        log.info("API CALL → Get Role API Access By Role | roleId={}", roleId);

        List<ApiAccessByRole> result = repository.findByRole_Id(roleId);

        log.info("SUCCESS → Retrieved {} API access records for role {}", result.size(), roleId);
        return result;
    }

    @Override
    public ApiAccessByRole save(Integer roleId, Integer apiId) {

        if (repository.findByRoleIdAndApiId(roleId, apiId).isPresent()) {
            log.error("DUPLICATE ENTRY → Role API already exists | roleId={} apiId={}",
                    roleId, apiId);
            throw new IllegalStateException("Functionality already assigned to this role");
        }



        RoleMaster role = roleRepo.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        ApiEndpoint api = apiRepo.findById(apiId)
                .orElseThrow(() -> new RuntimeException("API not found"));

        ApiAccessByRole entity = new ApiAccessByRole();
        entity.setRole(role);
        entity.setApi(api);
        entity.setStatus(true);
        entity.setCreatedBy(
                currentUser.getCurrentEmployeeOrThrow().getId().toString()
        );

        return repository.save(entity);
    }

    @Override
    public ApiAccessByRole changeStatus(Integer id, Boolean status) {
        log.info("API CALL → Change Role API Access Status | id={} status={}", id, status);

        ApiAccessByRole entity = repository.findById(id)
                .orElseThrow(() -> {
                    log.error("API access entry not found | id={}", id);
                    return new RuntimeException("Entry not found");
                });

        log.debug("Updating API access status | id={} oldStatus={} newStatus={}",
                id, entity.getStatus(), status);

        entity.setStatus(status);
        entity.setUpdatedBy(String.valueOf(currentUser.getCurrentEmployeeOrThrow().getId()));
        entity.setUpdatedOn(new Timestamp(System.currentTimeMillis()));

        ApiAccessByRole updatedEntity = repository.save(entity);

        log.info("SUCCESS → Updated Role API Access Status | id={} newStatus={}", id, status);
        return updatedEntity;
    }


    //================================ ApiEndpointService ============================================


    @Override
    public ApiEndpoint createApiEndpoint(ApiEndpoint apiEndpoint) {
        log.info("API CALL → Create API Endpoint | name={} endpointType={} method={} endpoint={}",
                apiEndpoint.getEndpoint(),
                apiEndpoint.getEndpointType().getName(),
                apiEndpoint.getMethod(),
                apiEndpoint.getEndpoint());

        apiEndpoint.setCreatedOn(new Timestamp(System.currentTimeMillis()));

        ApiEndpoint savedEndpoint = apiEndpointRepository.save(apiEndpoint);

        log.info("SUCCESS → Created API Endpoint | id={} name={} endpoint={}",
                savedEndpoint.getId(), savedEndpoint.getEndpoint(), savedEndpoint.getEndpoint());
        return savedEndpoint;
    }


    @Override
    public ApiEndpoint updateApiEndpoint(Integer id, ApiEndpoint request) {
        log.info("API CALL → Update API Endpoint | id={}", id);

        ApiEndpoint existing = apiEndpointRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("API Endpoint not found | id={}", id);
                    return new RuntimeException("API Endpoint not found");
                });

        log.debug("Updating API endpoint | id={} oldName={} newName={} oldEndpoint={} newEndpoint={}",
                id, existing.getEndpoint(), request.getEndpoint(), existing.getEndpoint(), request.getEndpoint());

        existing.setMethod(request.getMethod());
        existing.setEndpoint(request.getEndpoint());
        existing.setController(request.getController());
        existing.setWorking(request.getWorking());
        existing.setUpdatedBy(String.valueOf(currentUser.getCurrentEmployeeOrThrow().getId()));
        existing.setUpdatedOn(new Timestamp(System.currentTimeMillis()));

        ApiEndpoint updatedEndpoint = apiEndpointRepository.save(existing);

        log.info("SUCCESS → Updated API Endpoint | id={} name={} endpoint={}",
                id, updatedEndpoint.getEndpoint(), updatedEndpoint.getEndpoint());
        return updatedEndpoint;
    }


    @Override
    public ApiEndpoint getByIdApiEndpoint(Integer id) {
        log.info("API CALL → Get API Endpoint By ID | id={}", id);

        ApiEndpoint endpoint = apiEndpointRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("API Endpoint not found | id={}", id);
                    return new RuntimeException("API Endpoint not found");
                });

        log.info("SUCCESS → Retrieved API Endpoint | id={} name={} endpoint={}",
                id, endpoint.getEndpoint(), endpoint.getEndpoint());
        return endpoint;
    }


    @Override
    public List<ApiEndpoint> getAllApiEndpoint() {
        log.info("API CALL → Get All API Endpoints");

        List<ApiEndpoint> endpoints = apiEndpointRepository.findAll();

        log.info("SUCCESS → Retrieved {} API endpoints", endpoints.size());
        return endpoints;
    }

    @Override
    public List<ApiEndpoint> getByEndpointTypeId(Integer endpointTypeId) {
        log.info("API CALL → Get API Endpoints By Endpoint Type | endpointTypeId={}", endpointTypeId);

        List<ApiEndpoint> endpoints = apiEndpointRepository.findByEndpointType_Id(endpointTypeId);

        log.info("SUCCESS → Retrieved {} API endpoints for endpoint type {}",
                endpoints.size(), endpointTypeId);
        return endpoints;
    }


    //================================ ApiEndpointTypeService ============================================


    @Override
    public List<ApiEndpointType> getAllEndpointTypes() {
        log.info("API CALL → Get All API Endpoint Types");

        List<ApiEndpointType> endpointTypes = apiEndpointTypeRepository.findAll();

        log.info("SUCCESS → Retrieved {} API endpoint types", endpointTypes.size());
        return endpointTypes;
    }
}