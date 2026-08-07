//package com.dmsBackend.controller;
//
//import com.dmsBackend.service.ScannerService;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.core.io.InputStreamResource;
//import org.springframework.core.io.Resource;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.io.File;
//import java.io.FileInputStream;
//import java.nio.file.Files;
//
//@RestController
//@RequestMapping("/api/scan")
//@Slf4j
//public class ScanController {
//
//    private final ScannerService scannerService;
//
//    public ScanController(ScannerService scannerService) {
//        this.scannerService = scannerService;
//    }
//
//    /**
//     * Example:
//     * POST http://localhost:8080/api/scan/pdf?totalPages=5&scanType=multiple&fileName=myDoc
//     * scanType: oneByOne | multiple
//     */
//    @PostMapping(value = "/pdf")
//    public ResponseEntity<Resource> scanToPdf(
//            @RequestParam int totalPages,
//            @RequestParam(defaultValue = "oneByOne") String scanType,
//            @RequestParam(defaultValue = "scanned_output") String fileName
//    ) throws Exception {
//
//        log.info("✅ Scanner controller called for totalPages = {}, scanType = {}, fileName = {}",
//                totalPages, scanType, fileName);
//
//        // Ensure .pdf extension
//        String safeFileName = fileName.endsWith(".pdf") ? fileName : fileName;
//
//        File pdfFile = scannerService.scanToPdf(totalPages, scanType, safeFileName);
//
//        // Wrap in InputStreamResource (subclass of Resource)
//        Resource resource = new InputStreamResource(new FileInputStream(pdfFile));
//
//        return ResponseEntity.ok()
//                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + pdfFile.getName())
//                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "Content-Disposition")
//                .contentLength(pdfFile.length())
//                .contentType(MediaType.APPLICATION_PDF)
//                .body(resource);
//    }
//
//}
