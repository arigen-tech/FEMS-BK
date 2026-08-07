package com.dmsBackend.controller;

import com.dmsBackend.response.DashboardResponse;
import com.dmsBackend.service.DashboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    /**
     * Fetch dashboard statistics for the given employee ID.
     *
     * @param employeeId the ID of the employee.
     * @return a response entity containing the dashboard data.
     */
    @GetMapping("getAllCount/{employeeId}")
    public ResponseEntity<DashboardResponse> getDashboardData(
            @PathVariable String employeeId) {

        log.info("Dashboard data request received for EmployeeId={}", employeeId);

        try {
            DashboardResponse dashboardResponse =
                    dashboardService.getAllUsers(employeeId);

            log.info("Dashboard data fetched successfully for EmployeeId={}", employeeId);
            return ResponseEntity.ok(dashboardResponse);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid input while fetching dashboard data. EmployeeId={}, Error={}",
                    employeeId, e.getMessage());
            return ResponseEntity.badRequest().body(null);

        } catch (RuntimeException e) {
            log.error("Dashboard data not found for EmployeeId={}, Error={}",
                    employeeId, e.getMessage());
            return ResponseEntity.status(404).body(null);

        } catch (Exception e) {
            log.error("Unexpected error while fetching dashboard data for EmployeeId={}",
                    employeeId, e);
            return ResponseEntity.status(500).body(null);
        }
    }
}
