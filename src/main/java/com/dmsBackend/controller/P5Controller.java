package com.dmsBackend.controller;


import com.dmsBackend.ArchiveWithLTO9.LtfsRetrieveFiles;
import com.dmsBackend.P5Archive.*;
import com.dmsBackend.response.CartridgeInfo;
import com.dmsBackend.utils.DetectCurrCartridge;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/p5/apis")
@RequiredArgsConstructor
@Slf4j
public class P5Controller {
    private final P5RestoreApiService restoreApiService;
    private final ArchiveDashboardP5Service dashboardService;

    private final LtfsRetrieveFiles ltfsRetrieveFiles;

    @Value("${document.storage.path}")
    private String restoreBasePath;

    @PostMapping("/restore/{documentId}")
    public ResponseEntity<?> restoreDocument(
            @PathVariable Integer documentId
    ) {

        ltfsRetrieveFiles.restoreFile(documentId);

        return ResponseEntity.ok("Restore request submitted successfully");
    }

    @PostMapping("/restoreBulk/{policyId}")
    public ResponseEntity<String> restoreDocumentsBulk(
            @PathVariable Integer policyId
    ) {

        ltfsRetrieveFiles.restoreBulk(policyId);

        return ResponseEntity
                .accepted()
                .body("Your Policy Restore Request is Accepted");
    }



    @GetMapping("/counts")
    public ResponseEntity<DashboardCountResponse> getDashboardCounts() {
        return ResponseEntity.ok(dashboardService.getDashboardCounts());
    }


    @GetMapping("/dashboardByPolicy/{policyId}")
    public ResponseEntity<List<P5DashboardRes2>> getDashboardByPolicy(
            @PathVariable Integer policyId) {

        return ResponseEntity.ok(dashboardService.getDashboardByPolicy(policyId));
    }

    @GetMapping("/dashboardByPolicy/{policyId}/document/{headerId}")
    public ResponseEntity<List<P5DashboardRes3>> getDashboardByDocumentHeader(
            @PathVariable Integer policyId,
            @PathVariable Integer headerId) {

        return ResponseEntity.ok(
                dashboardService.getDashboardByDocumentHeader(policyId, headerId)
        );
    }


    @GetMapping("/files")
    public ResponseEntity<List<P5DashboardRes4>> getDocumentDetails(
            @RequestParam("ids") String idsCsv) {

        List<P5DashboardRes4> documents = dashboardService.getDocumentDetailsByIds(idsCsv);
        return ResponseEntity.ok(documents);
    }


    @GetMapping("/cartridge/current")
    public CartridgeInfo getInfoCurrCartage(
            @RequestParam(defaultValue = "E:") String drive) {

        return DetectCurrCartridge.detect(drive);
    }

}
