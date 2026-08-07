package com.dmsBackend.service;

import com.dmsBackend.entity.ApiAccessByRole;
import com.dmsBackend.entity.ApiEndpoint;
import com.dmsBackend.entity.ApiEndpointType;

import java.util.List;

public interface ApiAccessByRoleService {
    List<ApiAccessByRole> getAll();

    List<ApiAccessByRole> getByRole(Integer roleId);

    ApiAccessByRole save(
            Integer roleId,
            Integer apiId
    );

    ApiAccessByRole changeStatus(Integer id, Boolean status);




    //================================ ApiEndpointService ============================================
    ApiEndpoint createApiEndpoint(ApiEndpoint apiEndpoint);

    ApiEndpoint updateApiEndpoint(Integer id, ApiEndpoint apiEndpoint);

    ApiEndpoint getByIdApiEndpoint(Integer id);

    List<ApiEndpoint> getAllApiEndpoint();
    List<ApiEndpoint> getByEndpointTypeId(Integer endpointTypeId);


    //================================ ApiEndpointTypeService ============================================


    List<ApiEndpointType> getAllEndpointTypes();
}
