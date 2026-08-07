package com.dmsBackend.security;

import com.dmsBackend.entity.ApiAccessByRole;
import com.dmsBackend.entity.Employee;
import com.dmsBackend.repository.ApiAccessByRoleRepository;
import com.dmsBackend.repository.EmployeeRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class RoleApiAuthorizationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RoleApiAuthorizationFilter.class);

    private final ApiAccessByRoleRepository apiAccessRepo;
    private final EmployeeRepository employeeRepository;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public RoleApiAuthorizationFilter(
            ApiAccessByRoleRepository apiAccessRepo,
            EmployeeRepository employeeRepository
    ) {
        this.apiAccessRepo = apiAccessRepo;
        this.employeeRepository = employeeRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        log.info("========== RoleApiAuthorizationFilter START ==========");

        String requestPath = request.getRequestURI();
        String method = request.getMethod();

        log.info("Incoming Request -> Method: {}, Path: {}", method, requestPath);

        // Allow preflight requests
        if ("OPTIONS".equalsIgnoreCase(method)) {
            log.info("OPTIONS request detected. Skipping authorization.");
            filterChain.doFilter(request, response);
            return;
        }

        // Public APIs
        if (isPublicApi(requestPath)) {
            log.info("Public API detected -> {}", requestPath);
            filterChain.doFilter(request, response);
            return;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            log.warn("No authentication found. Allowing request to proceed.");
            filterChain.doFilter(request, response);
            return;
        }

        log.info("Authentication object found.");

        String email = ((User) auth.getPrincipal()).getUsername();
        log.info("Authenticated user email -> {}", email);

        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        Integer roleId = employee.getRole().getId();
        log.info("Employee found -> Name: {}, RoleId: {}", employee.getName(), roleId);

        List<ApiAccessByRole> rules = apiAccessRepo.findByRoleAndMethodFlexible(roleId, method);
        log.info("Rules fetched from DB -> count: {}", rules.size());

        String action = request.getParameter("action");
        String status = request.getParameter("status");
        String isDeleted = request.getParameter("isDeleted");

        log.info("Request Parameters -> action: {}, status: {}, isDeleted: {}", action, status, isDeleted);

        Optional<ApiAccessByRole> matchedRule = rules.stream()
                .filter(rule -> {

                    String apiPattern = rule.getApi().getEndpoint();
                    String working = rule.getApi().getWorking();

                    log.debug("Checking Rule -> Endpoint: {}, Working: {}", apiPattern, working);

                    /* =========================
                       VIEW DOCUMENTS
                       ========================= */
                    if (pathMatcher.match("/Dms/api/documents/view/**", requestPath)) {
                        log.debug("Matched VIEW document path");
                        return apiPattern.equals("/Dms/api/documents/view/**");
                    }

                    /* =========================
                       DOWNLOAD / VIEW
                       ========================= */
                    if (pathMatcher.match("/Dms/api/documents/download/**", requestPath)) {

                        log.debug("Matched DOWNLOAD path");

                        if ("view".equalsIgnoreCase(action)) {
                            log.debug("Action=view -> mapping to VIEW permission");
                            return apiPattern.equals("/Dms/api/documents/view/**");
                        }

                        return apiPattern.equals("/Dms/api/documents/download/**");
                    }

                    /* =========================
                       APPROVE / REJECT
                       ========================= */
                    if (pathMatcher.match("/Dms/api/documents/**/approval-status", requestPath)) {

                        log.debug("Matched APPROVAL endpoint");

                        if ("APPROVED".equalsIgnoreCase(status)) {
                            log.debug("Approval action detected");
                            return apiPattern.equals("/Dms/api/documents/**/approval-status")
                                    && "Approve Document".equalsIgnoreCase(working);
                        }

                        if ("REJECTED".equalsIgnoreCase(status)) {
                            log.debug("Reject action detected");
                            return apiPattern.equals("/Dms/api/documents/**/approval-status")
                                    && "Reject Document".equalsIgnoreCase(working);
                        }

                        log.warn("Invalid or missing status parameter.");
                        return false;
                    }

                    /* =========================
                       TRASH / UNTRASH
                       ========================= */
                    if (pathMatcher.match("/Dms/api/documents/delete-status/**", requestPath)) {

                        log.debug("Matched DELETE STATUS endpoint");

                        if ("true".equalsIgnoreCase(isDeleted)) {
                            log.debug("Trash document operation");
                            return apiPattern.equals("/Dms/api/documents/delete-status/**")
                                    && "Trash Document".equalsIgnoreCase(working);
                        }

                        if ("false".equalsIgnoreCase(isDeleted)) {
                            log.debug("Untrash document operation");
                            return apiPattern.equals("/Dms/api/documents/delete-status/**")
                                    && "Untrash Document".equalsIgnoreCase(working);
                        }

                        log.warn("Invalid isDeleted parameter.");
                        return false;
                    }

                    /* =========================
                       EMP ROLE APIs
                       ========================= */
                    if (apiPattern.contains("/Dms/api/EmpRole/")) {

                        log.debug("Checking EmpRole API rules");

                        String[] endpoints = apiPattern.split(",");

                        for (String ep : endpoints) {

                            ep = ep.trim();

                            log.debug("Checking endpoint {} against request {}", ep, requestPath);

                            if (pathMatcher.match(ep, requestPath)) {

                                log.debug("Endpoint matched -> {}", ep);

                                String[] allowedMethods = rule.getApi().getMethod().split(",");

                                for (String m : allowedMethods) {

                                    if (m.trim().equalsIgnoreCase(request.getMethod())) {

                                        log.info("EmpRole rule matched -> {} {}", request.getMethod(), requestPath);

                                        return true;
                                    }
                                }

                                log.debug("Endpoint matched but method mismatch -> {}", request.getMethod());
                            }
                        }

                        log.debug("No EmpRole rule matched");
                        return false;
                    }

                    /* =========================
                       DEFAULT MATCH
                       ========================= */

                    boolean matched = pathMatcher.match(apiPattern, requestPath);

                    log.debug("Default matching -> Pattern: {}, Result: {}", apiPattern, matched);

                    return matched;
                })
                .sorted((a, b) ->
                        b.getApi().getEndpoint().length()
                                - a.getApi().getEndpoint().length()
                )
                .findFirst();

        if (matchedRule.isPresent()) {

            log.info("Matched Rule -> Endpoint: {}, Status: {}",
                    matchedRule.get().getApi().getEndpoint(),
                    matchedRule.get().getStatus());

        } else {

            log.warn("No rule matched for this request.");
        }

        // BLOCK ACCESS
        if (matchedRule.isPresent()
                && Boolean.TRUE.equals(matchedRule.get().getStatus())) {

            log.error("ACCESS BLOCKED -> {} {}", method, requestPath);

            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("""
                {
                  "message": "You are not authorized to access this resource"
                }
            """);

            return;
        }

        log.info("Access granted -> {} {}", method, requestPath);

        filterChain.doFilter(request, response);

        log.info("========== RoleApiAuthorizationFilter END ==========");
    }

    private boolean isPublicApi(String path) {

        boolean isPublic =
                path.startsWith("/auth")
                        || path.startsWith("/swagger")
                        || path.startsWith("/v3/Dms/api-docs")
                        || path.equals("/error");

        if (isPublic) {
            log.debug("Public API allowed -> {}", path);
        }

        return isPublic;
    }
}