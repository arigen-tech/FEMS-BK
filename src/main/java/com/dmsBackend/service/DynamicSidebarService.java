package com.dmsBackend.service;

import com.dmsBackend.response.*;

import java.util.List;

public interface DynamicSidebarService {


    //-------------------------------------------------------UserApplications-------------------------------------------//

    public ApiResponse<List<UserApplicationResponse>> getAllApplications(int flag);
    public ApiResponse<UserApplicationResponse> getApplicationById(Long id);
    public ApiResponse<UserApplicationResponse> createApplication(UserApplicationRequest request);
    public ApiResponse<UserApplicationResponse> changeStatusById(Long id,String status);
    public ApiResponse<UserApplicationResponse> updateApplicationById(Long id,UserApplicationRequest request);
    public ApiResponse<List<UserApplicationResponse>> getAllApplicationsWithHashUrl(int flag);


    //--------------------------------------------------------MasApplication---------------------------------------------//

    public ApiResponse<MasApplicationResponse> createApplication(MasApplicationRequest request);
    public ApiResponse<MasApplicationResponse> getApplicationById(String id);
    public ApiResponse<List<MasApplicationResponse>> getAllMasApplications(int flag);
    public ApiResponse<List<MasApplicationResponse>> getAllByParentId(String parentId, Long templateId);
    public ApiResponse<MasApplicationResponse> updateApplication(String id, MasApplicationRequest request);
    public ApiResponse<String> updateMultipleApplicationStatuses(UpdateStatusRequest request);
    public ApiResponse<String> processBatchUpdates(BatchUpdateRequest request);
    public ApiResponse<List<MasApplicationResponse>> getAllParentApplications(int flag);



    //------------------------------------------------------------MasTemplate---------------------------------------------------//


    ApiResponse<List<MasTemplateResponse>> getAllTemplates(int flag);
    ApiResponse<MasTemplateResponse> getTemplateById(Long id);
    ApiResponse<MasTemplateResponse> createTemplate(MasTemplateRequest request);
    ApiResponse<MasTemplateResponse> updateTemplate(Long id, MasTemplateRequest request);
    ApiResponse<String> changeTemplateStatus(Long id, String status);



    //=========================================================RoleTemplate====================================================//


    ApiResponse<List<RoleTemplateResponse>> addOrUpdateRoleTemplates(RoleTemplateRequestList requestList);
    ApiResponse<List<RoleTemplateResponse>> getTemplatesByRoleId(Long roleId, int flag);




    //===========================================================TemplateApplication==============================================//


    ApiResponse<TemplateApplicationResponse> assignTemplateToApplication(TemplateApplicationRequest request);
    ApiResponse<String> changeTemplateApplicationStatus(Long id, String status);
    ApiResponse<List<TemplateApplicationResponse>> getAllTemplateApplications(int flag);
    ApiResponse<List<TemplateApplicationResponse>> getAllTemplateById(Long templateId);

    ApiResponse<List<UrlByRoleResponse>> getAllUrlByRoleIds(List<Long> roleIds);

}
