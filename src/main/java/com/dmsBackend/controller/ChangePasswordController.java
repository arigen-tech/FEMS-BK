package com.dmsBackend.controller;

import com.dmsBackend.response.ChangePasswordDTO;
import com.dmsBackend.service.ChangePasswordService;
import com.dmsBackend.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ChangePasswordController {

    @Autowired
    private ChangePasswordService changePasswordService;


    @Autowired
    private EmployeeService employeeService;

        @PostMapping("/change-password")
        public ResponseEntity<?> changePassword(@RequestBody ChangePasswordDTO changePasswordDTO) {
            try {
                employeeService.changePassword(
                        changePasswordDTO.getEmail(),
                        changePasswordDTO.getCurrentPassword(),
                        changePasswordDTO.getNewPassword()
                );
                return ResponseEntity.ok().body("Password changed successfully");
            } catch (RuntimeException e) {
                return ResponseEntity.badRequest().body(e.getMessage());
            }
        }
    }

//    @PostMapping("/send-otp")
//    public ResponseEntity<String> sendOtp(@RequestBody String email) {
//        try {
//            // Validate the email before processing (you may want to add more validation)
//            if (email == null || email.isEmpty()) {
//                return ResponseEntity.badRequest().body("Email is required.");
//            }
//
//            // Attempt to send the OTP
//            changePasswordService.sendOtp(email);
//            return ResponseEntity.ok("OTP sent successfully");
//        } catch (RuntimeException e) {
//            // Return error message from exception
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
//        }
//    }
//


