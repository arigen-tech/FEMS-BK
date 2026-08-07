package com.dmsBackend.service.Impl;

import com.dmsBackend.entity.*;
import com.dmsBackend.repository.*;
import com.dmsBackend.response.*;
import com.dmsBackend.service.DynamicSidebarService;
import com.dmsBackend.utils.ResponseUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicSidebarServiceImpl implements DynamicSidebarService {

    private final UserApplicationRepository userApplicationRepository;

    private final EmployeeRepository empRepo;

    private final TemplateApplicationRepo templateApplicationRepository;

    private final MasApplicationRepo masApplicationRepo;

    private final MasTemplateRepo masTemplateRepo;

    private final RoleTemplateRepo roleTemplateRepo;

    @PersistenceContext
    private EntityManager entityManager;

    private final DataSource dataSource;

    @Autowired
    private RoleTemplateRepository roleTemplateRepository;

    //---------------------------------------------UserApplication---------------------------------------------------------//

    @Override
    public ApiResponse<List<UserApplicationResponse>> getAllApplications(int flag) {

       try {
           List<UserApplication> userApplications;
           if(flag==0){
               userApplications= userApplicationRepository.findByStatusInIgnoreCase(List.of("y","n"));
           }else if(flag==1){
               userApplications= userApplicationRepository.findByStatusIgnoreCase("y");
           }else{
               return ResponseUtils.createFailureResponse(null, new TypeReference<>() {},"Invalid Flag Value ,Please use 0 or 1", HttpStatus.BAD_REQUEST.value());
           }

           List<UserApplicationResponse> list = userApplications.stream().map(this::mapToResponseUserApplication).toList();
           return ResponseUtils.createSuccessResponse(list, new TypeReference<List<UserApplicationResponse>>() {});

       } catch (Exception e) {
           log.error("Unexpected Error :: ",e);
           return ResponseUtils.createFailureResponse(null, new TypeReference<>() {},
                   "Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR.value());
       }

    }

    @Override
    public ApiResponse<UserApplicationResponse> getApplicationById(Long id) {
        try {

            Optional<UserApplication> byId = userApplicationRepository.findById(id);
            if(byId.isPresent()){
                return ResponseUtils.createSuccessResponse(mapToResponseUserApplication(byId.get()), new TypeReference<UserApplicationResponse>() {});
            }
              return  ResponseUtils.createNotFoundResponse("Invalid Application Id , Application Id not Found",HttpStatus.NOT_FOUND.value());
        } catch (Exception e) {
            log.error("Unexpected Error :: ",e);
            return ResponseUtils.createFailureResponse(null, new TypeReference<>() {},
                    "Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    @Override
    public ApiResponse<UserApplicationResponse> createApplication(UserApplicationRequest request) {
       try {
           Optional<Employee> currentUser = getCurrentUser();
           if(currentUser.isEmpty()){
               return  ResponseUtils.createFailureResponse(null, new TypeReference<>() {},"Current User Not Found",HttpStatus.UNAUTHORIZED.value());
           }
           UserApplication application=new UserApplication();
           application.setUrl(request.getUrl());
           application.setStatus("y");
           application.setUserAppName(request.getUserAppName());
           application.setLastChgBy(currentUser.get().getId());

           return  ResponseUtils.createSuccessResponse(mapToResponseUserApplication(userApplicationRepository.save(application)), new TypeReference<>() {});

       } catch (Exception e) {
           log.error("Unexpected Error :: ",e);
           return ResponseUtils.createFailureResponse(null, new TypeReference<>() {},
                   "Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR.value());
       }
    }

    @Override
    public ApiResponse<UserApplicationResponse> changeStatusById(Long id, String status) {
        try {
            Optional<Employee> currentUser = getCurrentUser();
            if (currentUser.isEmpty()) {
                return ResponseUtils.createFailureResponse(null, new TypeReference<>() {
                }, "Current User Not Found", HttpStatus.UNAUTHORIZED.value());
            }
            Optional<UserApplication> byId = userApplicationRepository.findById(id);
            if(byId.isPresent()){
                UserApplication application = byId.get();
                application.setStatus(status);
                application.setLastChgBy(currentUser.get().getId());
                return ResponseUtils.createSuccessResponse(mapToResponseUserApplication(userApplicationRepository.save(application)), new TypeReference<>() {});
            }
            return ResponseUtils.createNotFoundResponse("Invalid Application Id, Application ID not Found",HttpStatus.NOT_FOUND.value());
        } catch (Exception e) {
            log.error("Unexpected Error :: ",e);
            return ResponseUtils.createFailureResponse(null, new TypeReference<>() {},
                    "Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    @Override
    public ApiResponse<UserApplicationResponse> updateApplicationById(Long id, UserApplicationRequest request) {
        try {
            Optional<Employee> currentUser = getCurrentUser();
            if (currentUser.isEmpty()) {
                return ResponseUtils.createFailureResponse(null, new TypeReference<>() {
                }, "Current User Not Found", HttpStatus.UNAUTHORIZED.value());
            }
            Optional<UserApplication> byId = userApplicationRepository.findById(id);
            if(byId.isPresent()){
                UserApplication application = byId.get();
                application.setUrl(request.getUrl());
                application.setUserAppName(request.getUserAppName());
                application.setLastChgBy(currentUser.get().getId());
                return ResponseUtils.createSuccessResponse(mapToResponseUserApplication(userApplicationRepository.save(application)), new TypeReference<>() {});
            }
            return ResponseUtils.createNotFoundResponse("Invalid Application Id, Application ID not Found",HttpStatus.NOT_FOUND.value());
        }catch (Exception e) {
            log.error("Unexpected Error :: ",e);
            return ResponseUtils.createFailureResponse(null, new TypeReference<>() {},
                    "Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    @Override
    public ApiResponse<List<UserApplicationResponse>> getAllApplicationsWithHashUrl(int flag) {

        try {
                List<UserApplication> applications;

                if (flag == 1) {
                    applications = userApplicationRepository.findByStatusIgnoreCaseAndUrl("Y", "#");
                } else if (flag == 0) {
                    applications = userApplicationRepository.findByStatusInIgnoreCaseAndUrl(List.of("Y", "N"), "#");
                } else {
                    return ResponseUtils.createFailureResponse(null, new TypeReference<>() {}, "Invalid flag value. Use 0 or 1.", HttpStatus.BAD_REQUEST.value());
                }

                List<UserApplicationResponse> responses = applications.stream()
                        .map(this::mapToResponseUserApplication)
                        .collect(Collectors.toList());

                return ResponseUtils.createSuccessResponse(responses, new TypeReference<>() {});

        }catch (Exception e){
            log.error("Unexpected Error :: ",e);
            return ResponseUtils.createFailureResponse(null, new TypeReference<>() {},
                    "Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }


    private Optional<Employee> getCurrentUser() {
        String username=SecurityContextHolder.getContext().getAuthentication().getName();
        return  empRepo.findByEmail(username);
    }

    private UserApplicationResponse mapToResponseUserApplication(UserApplication entity){
        UserApplicationResponse response=new UserApplicationResponse();
        response.setId(entity.getId());
        response.setUrl(entity.getUrl());
        response.setStatus(entity.getStatus());
        response.setUserAppName(entity.getUserAppName());
        response.setLastChgBy(entity.getLastChgBy());
        response.setLastChgDate(entity.getLastChgDate());
        return  response;
    }


    //------------------------------------------------MasApplication---------------------------------------------------//


    //create

    @Override
    @Transactional
    public ApiResponse<MasApplicationResponse> createApplication(MasApplicationRequest request) {
        try {
            MasApplication application = new MasApplication();
            application.setName(request.getName());
            application.setParentId(request.getParentId());
            application.setUrl(request.getUrl());
            application.setStatus(request.getStatus());
            application.setSerialNo(request.getSerialNo());

            Long nextOrderNo = getNextOrderNo();
            Long sequenceNo = masApplicationRepo.getNextAppSequenceNo(request.getParentId());

            application.setOrderNo(nextOrderNo);
            application.setAppId("A" + nextOrderNo);
            application.setAppSequenceNo(sequenceNo);

            validateSerialNo(application);

            MasApplication savedApplication = masApplicationRepo.save(application);

            // update user_application status if already exists
            UserApplication existingUserApp = userApplicationRepository.findByUserAppName(request.getName());
            if (existingUserApp != null) {
                existingUserApp.setStatus("n");
                userApplicationRepository.save(existingUserApp);
            }

            return ResponseUtils.createSuccessResponse(
                    convertToResponse(savedApplication),
                    new TypeReference<>() {}
            );

        } catch (Exception e) {
            log.error("Unexpected Error :: {}", e.getMessage(), e);
            return ResponseUtils.createFailureResponse(
                    null,
                    new TypeReference<>() {},
                    e.getMessage(), // return actual cause for debugging
                    HttpStatus.INTERNAL_SERVER_ERROR.value()
            );
        }
    }

    @Transactional
    private Long getNextOrderNo() {
        try (Connection conn = dataSource.getConnection()) {
            String dbProductName = conn.getMetaData().getDatabaseProductName().toLowerCase();

            if (dbProductName.contains("postgresql")) {
                // PostgreSQL sequence
                try (PreparedStatement ps = conn.prepareStatement("SELECT nextval('mas_application_order_seq')");
                     ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getLong(1);
                    }
                }
            } else if (dbProductName.contains("mysql")) {
                // MySQL auto_increment table
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("INSERT INTO mas_application_order_seq VALUES ()");
                }

                try (PreparedStatement ps = conn.prepareStatement("SELECT LAST_INSERT_ID()");
                     ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getLong(1);
                    }
                }
            } else {
                throw new UnsupportedOperationException("Unsupported DB: " + dbProductName);
            }

            throw new RuntimeException("Failed to fetch next order number");

        } catch (Exception e) {
            log.error("Failed to generate next order number", e);
            throw new RuntimeException("Failed to generate next order number", e);
        }
    }
    private MasApplicationResponse convertToResponse(MasApplication app) {
        // map entity → response DTO
        MasApplicationResponse res = new MasApplicationResponse();
        res.setAppId(app.getAppId());
        res.setName(app.getName());
        res.setParentId(app.getParentId());
        res.setSerialNo(app.getSerialNo());
        res.setUrl(app.getUrl());
        res.setOrderNo(app.getOrderNo());
        res.setStatus(app.getStatus());
        res.setAppSequenceNo(app.getAppSequenceNo());
        return res;
    }

    //find ALL

    @Override
    public ApiResponse<List<MasApplicationResponse>> getAllMasApplications(int flag) {
        List<MasApplication> applications;

        if (flag == 1) {
            applications = masApplicationRepo.findByStatusIgnoreCase("y");
        } else if (flag == 0) {
            applications = masApplicationRepo.findByStatusInIgnoreCase(List.of("y", "n"));
        } else {
            return ResponseUtils.createFailureResponse(null, new TypeReference<List<MasApplicationResponse>>() {}, "Invalid flag value. Use 0 or 1.", 400);
        }

        // Convert entity list to response list
        List<MasApplicationResponse> responses = applications.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return ResponseUtils.createSuccessResponse(responses, new TypeReference<List<MasApplicationResponse>>() {});
    }

    //get By ID

    @Override
    public ApiResponse<MasApplicationResponse> getApplicationById(String id) {

        try {
            Optional<MasApplication> byId = masApplicationRepo.findById(id);
            if(byId.isPresent()){
                return ResponseUtils.createSuccessResponse(convertToResponse(byId.get()), new TypeReference<>() {});
            }
            return ResponseUtils.createNotFoundResponse("inavalid App ID , App ID not found",HttpStatus.NOT_FOUND.value());
        } catch (Exception e) {
            log.error("Unexpected Error :: ",e);
            return ResponseUtils.createFailureResponse(null, new TypeReference<>() {},
                    "Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    @Override
    public ApiResponse<List<MasApplicationResponse>> getAllByParentId(String parentId, Long templateId) {
        // First, fetch the parent application
        MasApplication parentApplication = masApplicationRepo.findById(parentId)
                .orElse(null);

        if (parentApplication == null) {
            return ResponseUtils.createFailureResponse(null, new TypeReference<>() {}, "Parent application not found with id: " + parentId, HttpStatus.NOT_FOUND.value());
        }

        // Create a list to hold all applications hierarchically
        List<MasApplicationResponse> responses = new ArrayList<>();

        // Add parent first with template status
        MasApplicationResponse parentResponse = convertToResponseWithTemplateStatus(parentApplication, templateId);

        // Recursively fetch all descendant applications (not just immediate children)
        fetchDescendants(parentId, templateId, parentResponse);

        // Add the parent with all its nested children to the response
        responses.add(parentResponse);

        return ResponseUtils.createSuccessResponse(responses, new TypeReference<>() {});
    }

    /**
     * Recursively fetches all descendants for a given parent application
     * @param parentId The parent application ID
     * @param templateId The template ID to check assignments
     * @param parentResponse The parent response object to populate with children
     */
    private void fetchDescendants(String parentId, Long templateId, MasApplicationResponse parentResponse) {
        // Fetch direct children of this parent
        List<MasApplication> childApplications = masApplicationRepo.findByParentId(parentId);

        // If no children, set children to empty list rather than null for consistency
        if (childApplications.isEmpty()) {
            parentResponse.setChildren(new ArrayList<>());
            return;
        }

        // Process each child application
        List<MasApplicationResponse> childResponses = new ArrayList<>();
        for (MasApplication child : childApplications) {
            // Convert child to response with template status
            MasApplicationResponse childResponse = convertToResponseWithTemplateStatus(child, templateId);

            // Recursively fetch its descendants
            fetchDescendants(child.getAppId(), templateId, childResponse);

            // Add to children list
            childResponses.add(childResponse);
        }

        // Set children list on parent response
        parentResponse.setChildren(childResponses);
    }

    //Update by Id

    @Override
    public ApiResponse<MasApplicationResponse> updateApplication(String id, MasApplicationRequest request) {
        try {
            Optional<MasApplication> existingApplication = masApplicationRepo.findById(id);
            if (existingApplication.isPresent()) {
                MasApplication application = existingApplication.get();
                application.setName(request.getName());
                application.setParentId(request.getParentId());
                application.setUrl(request.getUrl());
                application.setStatus(request.getStatus());
                application.setSerialNo(request.getSerialNo());
                validateSerialNo(application);
                MasApplication updatedApplication = masApplicationRepo.save(application);
                return ResponseUtils.createSuccessResponse(convertToResponse(updatedApplication), new TypeReference<>() {});
            } else {
                return ResponseUtils.createNotFoundResponse("Application not found", 404);
            }
        } catch (Exception e) {
            log.error("Unexpected Error :: ",e);
            return ResponseUtils.createFailureResponse(null, new TypeReference<>() {},
                    "Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    @Override
    @Transactional
    public ApiResponse<String> updateMultipleApplicationStatuses(UpdateStatusRequest request) {
        try {
            List<UpdateStatusRequest.ApplicationStatusUpdate> updates = request.getApplications();

            if (updates == null || updates.isEmpty()) {
                return ResponseUtils.createFailureResponse(null, new TypeReference<>() {}, "No applications provided for update.", 400);
            }

            List<String> appIds = updates.stream().map(UpdateStatusRequest.ApplicationStatusUpdate::getAppId).toList();
            List<MasApplication> applications = masApplicationRepo.findAllById(appIds);

            if (applications.isEmpty()) {
                return ResponseUtils.createFailureResponse(null, new TypeReference<>() {}, "No matching applications found for the given IDs.", 404);
            }

            Map<String, String> statusMap = updates.stream()
                    .collect(Collectors.toMap(UpdateStatusRequest.ApplicationStatusUpdate::getAppId, UpdateStatusRequest.ApplicationStatusUpdate::getStatus));

            applications.forEach(app -> {
                String newStatus = statusMap.get(app.getAppId());
                if (newStatus != null && isValidStatus(newStatus)) {
                    app.setStatus(newStatus);
                    app.setLastChgDate(Instant.now());
                }
            });

            masApplicationRepo.saveAll(applications);

            return ResponseUtils.createSuccessResponse("Successfully updated " + applications.size() + " applications.", new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Unexpected Error :: ",e);
            return ResponseUtils.createFailureResponse(null, new TypeReference<>() {},
                    "Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    private boolean isValidStatus(String status) {
        return "Y".equalsIgnoreCase(status) || "N".equalsIgnoreCase(status);
    }

    @Override
    @Transactional
    public ApiResponse<String> processBatchUpdates(BatchUpdateRequest request) {
        try {
            List<String> errors = new ArrayList<>();
            List<String> successMessages = new ArrayList<>();

            // Validate and filter valid template assignments
            if (request.getTemplateApplicationAssignments() != null) {
                request.setTemplateApplicationAssignments(
                        request.getTemplateApplicationAssignments().stream()
                                .filter(assignment ->
                                        assignment.getTemplateId() != null &&
                                                StringUtils.hasText(assignment.getAppId()) &&
                                                assignment.getLastChgBy() != null)
                                .collect(Collectors.toList())
                );
            }

            // Validate and filter valid status updates
            List<BatchUpdateRequest.ApplicationStatusUpdate> statusUpdates = new ArrayList<>();
            if (request.getApplicationStatusUpdates() != null) {
                statusUpdates = request.getApplicationStatusUpdates().stream()
                        .filter(update ->
                                update.getTemplateId() != null &&
                                        StringUtils.hasText(update.getAppId()) &&
                                        StringUtils.hasText(update.getStatus()))
                        .peek(update -> update.setStatus(update.getStatus().toLowerCase()))
                        .toList();
            }

            if ((request.getTemplateApplicationAssignments() == null || request.getTemplateApplicationAssignments().isEmpty()) &&
                    statusUpdates.isEmpty()) {
                return ResponseUtils.createFailureResponse(
                        null,
                        new TypeReference<>() {},
                        "No valid updates or assignments provided.",
                        400
                );
            }

            // Process template application assignments
            Set<String> processedAppTemplateKeys = new HashSet<>();
            if (request.getTemplateApplicationAssignments() != null && !request.getTemplateApplicationAssignments().isEmpty()) {
                for (BatchUpdateRequest.TemplateApplicationAssignment assignment : request.getTemplateApplicationAssignments()) {
                    try {
                        String appId = assignment.getAppId();
                        Long templateId = assignment.getTemplateId();
                        String key = appId + "_" + templateId;

                        Optional<TemplateApplication> optionalTemplateApp =
                                templateApplicationRepository.findByTemplate_IdAndApp_AppId(templateId, appId);

                        TemplateApplication templateApp;
                        if (optionalTemplateApp.isPresent()) {
                            templateApp = optionalTemplateApp.get();
                            templateApp.setStatus(assignment.getStatus().toLowerCase());
                            templateApp.setOrderNo(assignment.getOrderNo());
                            templateApp.setLastChgBy(assignment.getLastChgBy());
                            templateApp.setLastChgDate(Instant.now());
                            log.info("Updated template assignment for appId={}, templateId={}", appId, templateId);
                        } else {
                            // Create new assignment
                            templateApp = new TemplateApplication();
                            templateApp.setApp(masApplicationRepo.findById(appId).orElseThrow(() ->
                                    new IllegalArgumentException("App not found: " + appId)));
                            templateApp.setTemplate(masTemplateRepo.findById(templateId).orElseThrow(() ->
                                    new IllegalArgumentException("Template not found: " + templateId)));
                            templateApp.setOrderNo(assignment.getOrderNo());
                            templateApp.setStatus(assignment.getStatus().toLowerCase());
                            templateApp.setLastChgBy(assignment.getLastChgBy());
                            templateApp.setLastChgDate(Instant.now());
                            log.info("Created new template assignment for appId={}, templateId={}", appId, templateId);
                        }

                        templateApplicationRepository.save(templateApp);
                        successMessages.add("Processed assignment for appId=" + appId + ", templateId=" + templateId);
                        processedAppTemplateKeys.add(key);

                    } catch (Exception ex) {
                        log.error("Error processing template assignment: {}", ex.getMessage(), ex);
                        errors.add("Failed to process assignment for appId=" + assignment.getAppId());
                    }
                }
            }
            // Process status updates (only those not already handled above)
            int updatedCount = 0;
            List<String> notFoundApps = new ArrayList<>();

            for (BatchUpdateRequest.ApplicationStatusUpdate update : statusUpdates) {
                String appId = update.getAppId();
                Long templateId = update.getTemplateId();
                String status = update.getStatus();
                String key = appId + "_" + templateId;

                if (processedAppTemplateKeys.contains(key)) continue;

                Optional<TemplateApplication> optionalTemplateApp =
                        templateApplicationRepository.findByTemplate_IdAndApp_AppId(templateId, appId);

                if (optionalTemplateApp.isPresent()) {
                    TemplateApplication templateApp = optionalTemplateApp.get();
                    String oldStatus = templateApp.getStatus();
                    templateApp.setStatus(status);
                    templateApp.setLastChgDate(Instant.now());
                    templateApplicationRepository.save(templateApp);
                    updatedCount++;
                    log.info("Updated status for appId={}, templateId={} from {} to {}", appId, templateId, oldStatus, status);
                } else {
                    notFoundApps.add("appId=" + appId + ", templateId=" + templateId);
                    log.warn("No template application found for appId={}, templateId={}", appId, templateId);
                }
            }

            if (updatedCount > 0) {
                successMessages.add("Updated status for " + updatedCount + " template applications");
            }
            if (!notFoundApps.isEmpty()) {
                successMessages.add("Skipped status updates for unassigned template applications: " + String.join(", ", notFoundApps));
            }

            // Return response
            if (!errors.isEmpty()) {
                return ResponseUtils.createFailureResponse(
                        null,
                        new TypeReference<>() {},
                        String.join(". ", errors),
                        !successMessages.isEmpty() ? 207 : 400  // 207 = Multi-Status (Partial Success)
                );
            }

            String finalMessage = String.join(". ", successMessages);
            return ResponseUtils.createSuccessResponse(
                    finalMessage.isEmpty() ? "No updates processed." : finalMessage,
                    new TypeReference<>() {}
            );
        } catch (Exception e) {
            log.error("Unexpected Error :: ",e);
            return ResponseUtils.createFailureResponse(null, new TypeReference<>() {},
                    "Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    @Override
    public ApiResponse<List<MasApplicationResponse>> getAllParentApplications(int flag) {
        try {
            List<MasApplication> applications;

            // First get applications based on status flag
            if (flag == 1) {
                applications = masApplicationRepo.findByStatusIgnoreCase("Y");
            } else if (flag == 0) {
                applications = masApplicationRepo.findAll();
            } else {
                return ResponseUtils.createFailureResponse(null, new TypeReference<>() {}, "Invalid flag value. Use 0 or 1.", 400);
            }


            // Filter to get parent applications (where parentId is "0" OR url is "#")
            applications = applications.stream()
                    .filter(app -> (app.getParentId() != null && app.getParentId().equals("0"))
                            || (app.getUrl() != null && app.getUrl().equals("#")))
                    .collect(Collectors.toList());


            List<MasApplicationResponse> responses = applications.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());

            return ResponseUtils.createSuccessResponse(responses, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Unexpected Error :: ",e);
            return ResponseUtils.createFailureResponse(null, new TypeReference<>() {},
                    "Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }




//Convert To response with Template status

    private MasApplicationResponse convertToResponseWithTemplateStatus(MasApplication application, Long templateId) {
        MasApplicationResponse response = convertToResponse(application);

        // Check if this application is assigned to the template
        TemplateApplication templateApp = templateApplicationRepository.findByTemplateAndApp(templateId, application.getAppId())
                .orElse(null);

        if (templateApp != null) {
            response.setAssigned(true);
            response.setStatus(templateApp.getStatus()); // "Y" or "N"
        } else {
            response.setAssigned(false);
            response.setStatus("n"); // Default to "N" if not assigned
        }

        return response;
    }

//convert to response

//    private MasApplicationResponse convertToResponse(MasApplication application){
//        MasApplicationResponse response = new MasApplicationResponse();
//        response.setAppId(application.getAppId());
//        response.setName(application.getName());
//        response.setParentId(application.getParentId());
//        response.setUrl(application.getUrl());
//        response.setOrderNo(application.getOrderNo());
//        response.setStatus(application.getStatus());
//        response.setLastChgDate(application.getLastChgDate());
//        response.setAppSequenceNo(application.getAppSequenceNo());
//        return response;
//    }



    //------------------------------------------------------MasTemplate--------------------------------------------//



    @Override
    public ApiResponse<List<MasTemplateResponse>> getAllTemplates(int flag) {
        try {
            List<MasTemplate> templates;

            if (flag == 1) {
                templates = masTemplateRepo.findByStatusIgnoreCase("Y");
            } else if (flag == 0) {
                templates = masTemplateRepo.findByStatusInIgnoreCase(List.of("Y", "N"));
            } else {
                return ResponseUtils.createFailureResponse(null, new TypeReference<>() {}, "Invalid flag value. Use 0 or 1.", 400);
            }

            List<MasTemplateResponse> responses = templates.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());

            return ResponseUtils.createSuccessResponse(responses, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Error in getAllTemplates(): ", e);
            return ResponseUtils.createFailureResponse(null, new TypeReference<>() {}, "Internal server error", 500);
        }
    }


    public ApiResponse<MasTemplateResponse> getTemplateById(Long id) {
        try {
            Optional<MasTemplate> template = masTemplateRepo.findById(id);
            return template.map(value -> ResponseUtils.createSuccessResponse(convertToResponse(value), new TypeReference<>() {}))
                    .orElseGet(() -> ResponseUtils.createNotFoundResponse("Template not found", 404));
        }catch (Exception e){
            log.error("Error in getTemplateById(): ", e);
            return ResponseUtils.createFailureResponse(null, new TypeReference<>() {}, "Internal server error", 500);
        }
    }

    public ApiResponse<MasTemplateResponse> createTemplate(MasTemplateRequest request) {

        try {
            Optional<Employee> currentUser = getCurrentUser();
            if(currentUser.isEmpty()){
                return  ResponseUtils.createFailureResponse(null, new TypeReference<>() {},"Current User Not Found",HttpStatus.UNAUTHORIZED.value());
            }

            MasTemplate template = new MasTemplate();
            template.setTemplateCode(request.getTemplateCode());
            template.setTemplateName(request.getTemplateName());
            template.setStatus("y"); // Default status to "Y"
            template.setLastChgBy(currentUser.get().getId());
            template.setLastChgDate(Instant.now());
            template.setBranchId(currentUser.get().getBranch().getId());
            template.setDepartment(currentUser.get().getDepartment());


            MasTemplate savedTemplate = masTemplateRepo.save(template);
            return ResponseUtils.createSuccessResponse(convertToResponse(savedTemplate), new TypeReference<>() {});
        }catch (Exception e){
            log.error("Error in createTemplate(): ", e);
            return ResponseUtils.createFailureResponse(null, new TypeReference<>() {}, "Internal server error", 500);
        }
    }

    public ApiResponse<MasTemplateResponse> updateTemplate(Long id, MasTemplateRequest request) {
        try {
            Optional<MasTemplate> existingTemplate = masTemplateRepo.findById(id);

            Optional<Employee> currentUser = getCurrentUser();
            if(currentUser.isEmpty()){
                return  ResponseUtils.createFailureResponse(null, new TypeReference<>() {},"Current User Not Found",HttpStatus.UNAUTHORIZED.value());
            }
            if (existingTemplate.isPresent()) {
                MasTemplate template = existingTemplate.get();
                template.setTemplateCode(request.getTemplateCode());
                template.setTemplateName(request.getTemplateName());
                template.setLastChgBy(currentUser.get().getId());
                template.setLastChgDate(Instant.now());
                template.setBranchId(currentUser.get().getBranch().getId());

                MasTemplate updatedTemplate = masTemplateRepo.save(template);
                return ResponseUtils.createSuccessResponse(convertToResponse(updatedTemplate), new TypeReference<>() {});
            } else {
                return ResponseUtils.createNotFoundResponse("Template not found", 404);
            }

        }catch (Exception e){
            log.error("Error in updateTemplate(): ", e);
            return ResponseUtils.createFailureResponse(null, new TypeReference<>() {}, "Internal server error", 500);
        }
    }

    public ApiResponse<String> changeTemplateStatus(Long id, String status) {
        try {
            if (!isValidStatus(status)) {
                return ResponseUtils.createFailureResponse(null, new TypeReference<>() {}, "Invalid status. Status should be 'Y' or 'N'", 400);
            }
            Optional<Employee> currentUser = getCurrentUser();
            if(currentUser.isEmpty()){
                return  ResponseUtils.createFailureResponse(null, new TypeReference<>() {},"Current User Not Found",HttpStatus.UNAUTHORIZED.value());
            }

            Optional<MasTemplate> template = masTemplateRepo.findById(id);
            if (template.isPresent()) {
                MasTemplate masTemplate = template.get();
                masTemplate.setStatus(status);
                masTemplate.setLastChgDate(Instant.now());
                masTemplate.setLastChgBy(currentUser.get().getId());
                masTemplate.setBranchId(currentUser.get().getBranch().getId());

                masTemplateRepo.save(masTemplate);
                return ResponseUtils.createSuccessResponse("Template status updated to '" + status + "'", new TypeReference<>() {});
            } else {
                return ResponseUtils.createNotFoundResponse("Template not found", 404);
            }
        }catch (Exception e){
            log.error("Error in changeTemplateStatus(): ", e);
            return ResponseUtils.createFailureResponse(null, new TypeReference<>() {}, "Internal server error", 500);
        }
    }

    private MasTemplateResponse convertToResponse(MasTemplate template) {
        MasTemplateResponse response = new MasTemplateResponse();
        response.setId(template.getId());
        response.setTemplateCode(template.getTemplateCode());
        response.setTemplateName(template.getTemplateName());
        response.setStatus(template.getStatus());
        response.setLastChgBy(template.getLastChgBy());
        response.setLastChgDate(template.getLastChgDate());
        response.setBranchId(template.getBranchId());
        return response;
    }



    //====================================================RoleTemplate==========================================================//



    @Override
    public ApiResponse<List<RoleTemplateResponse>> addOrUpdateRoleTemplates(RoleTemplateRequestList requestList) {
        List<RoleTemplateResponse> responses = requestList.getApplicationStatusUpdates().stream().map(request -> {
            // Retrieve or create MasTemplate
            MasTemplate template = masTemplateRepo.findById(request.getTemplateId())
                    .orElseThrow(() -> new RuntimeException("Template not found with ID: " + request.getTemplateId()));

            // Check if role-template mapping exists
            RoleTemplate roleTemplate = roleTemplateRepo.findByRoleIdAndTemplateId(
                    request.getRoleId(), request.getTemplateId()).orElse(null);

            if (roleTemplate == null) {
                // Create new RoleTemplate if it doesn't exist
                roleTemplate = new RoleTemplate();
                roleTemplate.setRoleId(request.getRoleId());
                roleTemplate.setTemplate(template);  // Set MasTemplate object
                roleTemplate.setStatus(request.getStatus());
                roleTemplate.setLastChgBy(request.getLastChgBy());
                roleTemplate.setLastChgDate(Instant.now());
            } else {
                // Update existing RoleTemplate
                roleTemplate.setStatus(request.getStatus());
                roleTemplate.setLastChgBy(request.getLastChgBy());
                roleTemplate.setLastChgDate(Instant.now());
            }

            RoleTemplate savedRoleTemplate = roleTemplateRepo.save(roleTemplate);
            return convertToResponse(savedRoleTemplate);
        }).collect(Collectors.toList());

        return ResponseUtils.createSuccessResponse(responses, new TypeReference<>() {});
    }

    @Override
    public ApiResponse<List<RoleTemplateResponse>> getTemplatesByRoleId(Long roleId, int flag) {
        List<RoleTemplate> templates = roleTemplateRepo.findByRoleId(roleId);

        if (flag == 1) {
            templates = templates.stream()
                    .filter(template -> "y".equals(template.getStatus()))
                    .toList();
        }

        List<RoleTemplateResponse> responses = templates.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return ResponseUtils.createSuccessResponse(responses, new TypeReference<>() {});
    }

    private RoleTemplateResponse convertToResponse(RoleTemplate roleTemplate) {
        RoleTemplateResponse response = new RoleTemplateResponse();
        response.setId(roleTemplate.getId());
        response.setRoleId(roleTemplate.getRoleId());
        response.setTemplateId(roleTemplate.getTemplate().getId()); // Get template ID from MasTemplate entity
        response.setStatus(roleTemplate.getStatus());
        response.setLastChgBy(roleTemplate.getLastChgBy());
        return response;
    }





    //=========================================================TemplateApplication=================================================//



    @Override
    public ApiResponse<TemplateApplicationResponse> assignTemplateToApplication(TemplateApplicationRequest request) {
        Optional<MasTemplate> templateOpt = masTemplateRepo.findById(request.getTemplateId());
        Optional<MasApplication> appOpt = masApplicationRepo.findById(request.getAppId());

        if (templateOpt.isEmpty() || appOpt.isEmpty()) {
            return ResponseUtils.createFailureResponse(null, new TypeReference<>() {}, "Template or Application not found", 404);
        }

        TemplateApplication templateApplication = new TemplateApplication();
        templateApplication.setTemplate(templateOpt.get());
        templateApplication.setApp(appOpt.get());
        templateApplication.setStatus("y");
        templateApplication.setLastChgDate(Instant.now());
        templateApplication.setLastChgBy(request.getLastChgBy());
        templateApplication.setOrderNo(request.getOrderNo());

        templateApplication = templateApplicationRepository.save(templateApplication);
        return ResponseUtils.createSuccessResponse(convertToResponse(templateApplication), new TypeReference<>() {});
    }

    @Override
    public ApiResponse<String> changeTemplateApplicationStatus(Long id, String status) {
        if (!isValidStatus(status)) {
            return ResponseUtils.createFailureResponse(null, new TypeReference<>() {}, "Invalid status. Status should be 'Y' or 'N'", 400);
        }

        Optional<TemplateApplication> templateApplicationOpt = templateApplicationRepository.findById(id);
        if (templateApplicationOpt.isPresent()) {
            TemplateApplication templateApplication = templateApplicationOpt.get();
            templateApplication.setStatus(status);
            templateApplication.setLastChgDate(Instant.now());
            templateApplicationRepository.save(templateApplication);
            return ResponseUtils.createSuccessResponse("Template Application status updated to '" + status + "'", new TypeReference<>() {});
        } else {
            return ResponseUtils.createNotFoundResponse("Template Application not found", 404);
        }
    }

    @Override
    public ApiResponse<List<TemplateApplicationResponse>> getAllTemplateApplications(int flag) {
        List<TemplateApplication> templateApplications;

        if (flag == 1) {
            templateApplications = templateApplicationRepository.findByStatusIgnoreCase("Y");
        } else if (flag == 0) {
            templateApplications = templateApplicationRepository.findByStatusInIgnoreCase(List.of("Y", "N"));
        } else {
            return ResponseUtils.createFailureResponse(null, new TypeReference<>() {}, "Invalid flag value. Use 0 or 1.", 400);
        }

        List<TemplateApplicationResponse> responses = templateApplications.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return ResponseUtils.createSuccessResponse(responses, new TypeReference<>() {});
    }

    @Override
    public ApiResponse<List<TemplateApplicationResponse>> getAllTemplateById(Long templateId) {
        Optional<MasTemplate> templateOpt = masTemplateRepo.findById(templateId);

        if (templateOpt.isEmpty()) {
            return ResponseUtils.createFailureResponse(null, new TypeReference<>() {}, "Template not found", 404);
        }

        List<TemplateApplication> templateApplications = templateApplicationRepository.findByTemplateId(templateId);

        List<TemplateApplicationResponse> responses = templateApplications.stream()
                .map(this::convertToResponse)
                .filter(app -> app.getAppId() != null && "0".equals(app.getParentId()))  // Filter parent applications
                .collect(Collectors.toList());

        return ResponseUtils.createSuccessResponse(responses, new TypeReference<>() {});
    }

    private TemplateApplicationResponse convertToResponse(TemplateApplication application) {
        TemplateApplicationResponse response = new TemplateApplicationResponse();

        // Set fields from the relationship entities
        if (application.getTemplate() != null) {
            response.setTemplateId(application.getTemplate().getId());
        }

        if (application.getApp() != null) {
            response.setAppId(application.getApp().getAppId());
            response.setAppName(application.getApp().getName());
            response.setParentId(application.getApp().getParentId());
        }

        response.setStatus(application.getStatus());
        response.setLastChgDate(application.getLastChgDate());

        return response;
    }


    //=================================final url======================================
    @Override
    public ApiResponse<List<UrlByRoleResponse>> getAllUrlByRoleIds(List<Long> roleIds) {
        // Step 1: Get all applications accessible to the provided roles
        Set<MasApplication> accessibleApps = new HashSet<>();

        for (Long roleId : roleIds) {
            List<RoleTemplate> roleTemplates = roleTemplateRepository.findByRoleIdAndStatusIgnoreCase(roleId, "y");

            for (RoleTemplate roleTemplate : roleTemplates) {
                Long templateId = roleTemplate.getTemplate().getId();
                List<TemplateApplication> activeTemplateApps = templateApplicationRepository.findByTemplateIdAndStatusIgnoreCase(templateId, "y");

                for (TemplateApplication templateApp : activeTemplateApps) {
                    MasApplication masApp = templateApp.getApp();
                    if (masApp != null && "y".equalsIgnoreCase(masApp.getStatus())) {
                        accessibleApps.add(masApp);
                    }
                }
            }
        }

        // Step 2: Create a map of all applications by their appId for easy access
        Map<String, MasApplication> appById = new HashMap<>();
        // Also create a map to store parent-child relationships
        Map<String, List<MasApplication>> childrenByParentId = new HashMap<>();

        for (MasApplication app : accessibleApps) {
            appById.put(app.getAppId(), app);

            // Organize children by parent ID
            String parentId = app.getParentId();
            if (parentId != null) {
                childrenByParentId.computeIfAbsent(parentId, k -> new ArrayList<>()).add(app);
            }
        }

        // Step 3: Build the hierarchical structure starting with root nodes (parentId = "0")
        List<UrlByRoleResponse> result = new ArrayList<>();
        List<MasApplication> rootApps = childrenByParentId.getOrDefault("0", Collections.emptyList());

        for (MasApplication rootApp : rootApps) {
            UrlByRoleResponse rootNode = convertToResponseUrl(rootApp);
            buildHierarchy(rootNode, rootApp.getAppId(), childrenByParentId);
            result.add(rootNode);
        }

        // Sort by order number if available
        result.sort(Comparator.comparing(r -> {
            MasApplication app = appById.get(r.getAppId());
            return app != null && app.getOrderNo() != null ? app.getOrderNo() : Long.MAX_VALUE;
        }));

        return Optional.of(result)
                .filter(list -> !list.isEmpty())
                .map(list -> ResponseUtils.createSuccessResponse(list, new TypeReference<List<UrlByRoleResponse>>() {}))
                .orElseGet(() -> ResponseUtils.createNotFoundResponse("No application menu found for given roles", 404));
    }

    private UrlByRoleResponse convertToResponseUrl(MasApplication app) {
        UrlByRoleResponse response = new UrlByRoleResponse();
        response.setAppId(app.getAppId());
        response.setName(app.getName());
        response.setSerialNo(app.getSerialNo());
        response.setUrl(app.getUrl());
        response.setChildren(new ArrayList<>());
        return response;
    }

    private void buildHierarchy(UrlByRoleResponse parent, String parentId,
                                Map<String, List<MasApplication>> childrenByParentId) {
        List<MasApplication> children = childrenByParentId.getOrDefault(parentId, Collections.emptyList());

        // Sort children by order number if available
        children.sort(Comparator.comparing(app -> app.getOrderNo() != null ? app.getOrderNo() : Long.MAX_VALUE));

        for (MasApplication childApp : children) {
            UrlByRoleResponse childNode = convertToResponseUrl(childApp);
            parent.getChildren().add(childNode);

            // Recursively build child's hierarchy
            buildHierarchy(childNode, childApp.getAppId(), childrenByParentId);
        }
    }

    //============================validation helper====================================
    public void validateSerialNo(MasApplication app) {
        List<MasApplication> siblings = masApplicationRepo.findByParentId(app.getParentId());

        boolean duplicateExists = siblings.stream()
                .anyMatch(s -> !s.getAppId().equals(app.getAppId()) &&
                        Objects.equals(s.getSerialNo(), app.getSerialNo()));

        if (duplicateExists) {
            throw new IllegalArgumentException(
                    String.format("Duplicate serial number %d found under parentId=%s",
                            app.getSerialNo(), app.getParentId()));
        }
    }

}
