//package com.dmsBackend.controller;
//
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.concurrent.TimeUnit;
//
//@RestController
//@RequestMapping("/api/lto")
//@Slf4j
//public class LtoController {
//
//    @Autowired
//    private LtoArchiveManager archiveManager;
//
//    @Autowired
//    private LtoRestoreService restoreService;
//
//    @Autowired
//    private P5SoapService p5SoapService;
//
//    @PostMapping("/archive/header/{headerId}")
//    public ResponseEntity<Map<String, Object>> archiveDocument(@PathVariable Integer headerId) {
//        try {
//            var future = archiveManager.archiveDocumentHeader(headerId);
//            var result = future.get(30, TimeUnit.SECONDS);
//
//            Map<String, Object> response = new HashMap<>();
//            response.put("success", result.isSuccess());
//            response.put("headerId", result.getHeaderId());
//            response.put("jobId", result.getJobId());
//            response.put("tapeBarcode", result.getTapeBarcode());
//            response.put("filesArchived", result.getFilesArchived());
//            response.put("message", result.isSuccess() ? "Archive initiated successfully" : result.getErrorMessage());
//
//            return ResponseEntity.ok(response);
//
//        } catch (Exception e) {
//            log.error("Archive request failed", e);
//
//            Map<String, Object> error = new HashMap<>();
//            error.put("success", false);
//            error.put("error", e.getMessage());
//            error.put("headerId", headerId);
//
//            return ResponseEntity.status(500).body(error);
//        }
//    }
//
//    @PostMapping("/restore/detail/{detailId}")
//    public ResponseEntity<Map<String, Object>> restoreDocument(@PathVariable Integer detailId) {
//        try {
//            var future = restoreService.restoreDocument(detailId);
//            var result = future.get(30, TimeUnit.SECONDS);
//
//            Map<String, Object> response = new HashMap<>();
//            response.put("success", result.isSuccess());
//            response.put("detailId", result.getDetailId());
//            response.put("jobId", result.getJobId());
//            response.put("restorePath", result.getRestorePath());
//            response.put("message", result.isSuccess() ? "Restore initiated successfully" : result.getErrorMessage());
//
//            return ResponseEntity.ok(response);
//
//        } catch (Exception e) {
//            log.error("Restore request failed", e);
//
//            Map<String, Object> error = new HashMap<>();
//            error.put("success", false);
//            error.put("error", e.getMessage());
//            error.put("detailId", detailId);
//
//            return ResponseEntity.status(500).body(error);
//        }
//    }
//
//    @GetMapping("/status/job/{jobId}")
//    public ResponseEntity<Map<String, Object>> getJobStatus(@PathVariable String jobId) {
//        try {
//            // Convert Map<String, String> to Map<String, Object>
//            Map<String, String> status = p5SoapService.getJobStatus(jobId);
//            Map<String, Object> statusObj = new HashMap<>(status);
//
//            return ResponseEntity.ok(statusObj);
//        } catch (Exception e) {
//            Map<String, Object> error = new HashMap<>();
//            error.put("error", e.getMessage());
//            return ResponseEntity.status(500).body(error);
//        }
//    }
//
//
//    @GetMapping("/tapes")
//    public ResponseEntity<Map<String, Object>> getAvailableTapes() {
//        try {
//            var tapes = p5SoapService.getAvailableTapes();
//
//            Map<String, Object> response = new HashMap<>();
//            response.put("success", true);
//            response.put("count", tapes.size());
//            response.put("tapes", tapes);
//
//            return ResponseEntity.ok(response);
//        } catch (Exception e) {
//            Map<String, Object> error = new HashMap<>();
//            error.put("success", false);
//            error.put("error", e.getMessage());
//            return ResponseEntity.status(500).body(error);
//        }
//    }
//
//    @GetMapping("/health")
//    public ResponseEntity<Map<String, Object>> checkHealth() {
//        Map<String, Object> health = new HashMap<>();
//
//        try {
//            // Test P5 connection
//            p5SoapService.login();
//            health.put("p5_connected", true);
//            health.put("status", "HEALTHY");
//        } catch (Exception e) {
//            health.put("p5_connected", false);
//            health.put("status", "UNHEALTHY");
//            health.put("error", e.getMessage());
//        }
//
//        health.put("timestamp", System.currentTimeMillis());
//
//        return ResponseEntity.ok(health);
//    }
//}