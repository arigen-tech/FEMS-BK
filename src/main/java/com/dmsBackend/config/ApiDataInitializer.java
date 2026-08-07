//package com.dmsBackend.config;
//
//import com.dmsBackend.entity.ApiAccessByRole;
//import com.dmsBackend.entity.ApiEndpoint;
//import com.dmsBackend.entity.ApiEndpointType;
//import com.dmsBackend.entity.RoleMaster;
//import com.dmsBackend.repository.ApiAccessByRoleRepository;
//import com.dmsBackend.repository.ApiEndpointRepository;
//import com.dmsBackend.repository.ApiEndpointTypeRepository;
//import com.dmsBackend.repository.RoleMasterRepository;
//import lombok.AllArgsConstructor;
//import lombok.Getter;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.List;
//
//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class ApiDataInitializer implements CommandLineRunner {
//
//    private final ApiEndpointTypeRepository endpointTypeRepo;
//    private final ApiEndpointRepository endpointRepo;
//    private final ApiAccessByRoleRepository accessRepo;
//    private final RoleMasterRepository roleRepo;
//
//    @Override
//    @Transactional
//    public void run(String... args) {
//
//        log.info("Initializing API master data...");
//
//        /* =================================================
//         * 1. API ENDPOINT TYPES
//         * ================================================= */
//        ApiEndpointType docType = getOrCreateType("Document Functionality");
//        ApiEndpointType authType = getOrCreateType("Authentication Functionality");
//        ApiEndpointType orgType  = getOrCreateType("Organization Functionality");
//
//        /* =================================================
//         * 2. API ENDPOINTS
//         * ================================================= */
//        List<ApiSeed> apis = List.of(
//                // ---- AUTH ----
//                new ApiSeed("POST","/auth/login","auth",authType,"Send login OTP"),
//                new ApiSeed("POST","/auth/verifyOtp","auth",authType,"Verify login OTP"),
//                new ApiSeed("POST","/auth/dms-login-outside","auth",authType,"Login without OTP"),
//                new ApiSeed("POST","/auth/forgot-password","auth",authType,"Send forgot password OTP"),
//                new ApiSeed("POST","/auth/verify-forgot-password-otp","auth",authType,"Verify forgot password OTP"),
//
//                // ---- ORGANIZATION ----
//                new ApiSeed("POST","/DepartmentMaster/save",
//                        "Department master", orgType,"create new department"),
//
//                // ---- DOCUMENTS ----
//                new ApiSeed("GET","/api/documents/view/**",
//                        "Documents Manage", docType,"View Documents"),
//                new ApiSeed("GET","/api/documents/download/**",
//                        "Documents Manage", docType,"Downloads Documents")
//        );
//
//        for (ApiSeed seed : apis) {
//            upsertApi(seed);
//        }
//
//        /* =================================================
//         * 3. API ACCESS BY ROLE
//         * ================================================= */
//        createAccess(4, "/DepartmentMaster/save", "POST", false);
//        createAccess(1, "/api/documents/download/**", "GET", true);
//
//        log.info("API master data initialization complete");
//    }
//
//    /* =================================================
//     * HELPERS
//     * ================================================= */
//
//    private ApiEndpointType getOrCreateType(String name) {
//        return endpointTypeRepo.findByName(name)
//                .orElseGet(() -> {
//                    ApiEndpointType t = new ApiEndpointType();
//                    t.setName(name);
//                    return endpointTypeRepo.save(t);
//                });
//    }
//
//    private void upsertApi(ApiSeed seed) {
//
//        if (endpointRepo.existsByMethodAndEndpoint(seed.method, seed.endpoint)) {
//            return;
//        }
//
//        ApiEndpoint api = new ApiEndpoint();
//        api.setCreatedBy("SYSTEM");
//        api.setMethod(seed.method);
//        api.setEndpoint(seed.endpoint);
//        api.setController(seed.controller);
//        api.setEndpointType(seed.type);
//        api.setWorking(seed.working);
//
//        endpointRepo.save(api);
//
//        log.info("Inserted API {} {}", seed.method, seed.endpoint);
//    }
//
//    private void createAccess(
//            Integer roleId,
//            String endpoint,
//            String method,
//            boolean status
//    ) {
//
//        RoleMaster role = roleRepo.findById(roleId)
//                .orElseThrow(() -> new RuntimeException("Role not found: " + roleId));
//
//        ApiEndpoint api = endpointRepo
//                .findByMethodAndEndpoint(method, endpoint)
//                .orElseThrow(() -> new RuntimeException("API not found: " + method + " " + endpoint));
//
//        if (accessRepo.existsByRoleAndApi(role, api)) {
//            return;
//        }
//
//        ApiAccessByRole access = new ApiAccessByRole();
//        access.setCreatedBy("SYSTEM");
//        access.setRole(role);
//        access.setApi(api);
//        access.setStatus(status);
//
//        accessRepo.save(access);
//    }
//
//    /* ================================================= */
//
//    @Getter
//    @AllArgsConstructor
//    private static class ApiSeed {
//        private String method;
//        private String endpoint;
//        private String controller;
//        private ApiEndpointType type;
//        private String working;
//    }

//}
