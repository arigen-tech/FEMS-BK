package com.dmsBackend.controller;

import com.dmsBackend.response.*;
import com.dmsBackend.response.ApiResponse;
import com.dmsBackend.service.ReportEntryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/report-entry")
public class ReportEntryController {

    @Autowired private ReportEntryService reportEntryService;

    @GetMapping("/pending")
    public ResponseEntity<List<ReportEntryListItem>> getPending() {
        return ResponseEntity.ok(reportEntryService.getMyPendingReportEntries());
    }

    @GetMapping("/case/{documentHeaderId}")
    public ResponseEntity<ReportEntryCaseResponse> getCase(@PathVariable Integer documentHeaderId) {
        return ResponseEntity.ok(reportEntryService.getCaseForReportEntry(documentHeaderId));
    }

    @PostMapping(value = "/save", consumes = "multipart/form-data")
    public ResponseEntity<com.dmsBackend.response.ApiResponse<MessageResponse>> save(
            @RequestParam Integer documentHeaderId,
            @RequestParam(required = false) String examinationStartDate,
            @RequestParam(required = false) String examinationEndDate,
            @RequestParam(required = false) Integer examinationMethodId,
            @RequestParam(required = false) String observations,
            @RequestParam(required = false) String scientificOpinion,
            @RequestParam(required = false) String examinationRemarks,
            @RequestParam(required = false) String reportDate,
            @RequestParam(required = false) String reportTitle,
            @RequestParam(required = false) String reportSummary,
            @RequestParam String status,
            @RequestParam(required = false) MultipartFile scientificReport,
            @RequestParam(required = false) List<MultipartFile> supportingDocuments) {

        ApiResponse<MessageResponse> response = reportEntryService.saveReportEntry(
                documentHeaderId,
                examinationStartDate,
                examinationEndDate,
                examinationMethodId,
                observations,
                scientificOpinion,
                examinationRemarks,
                reportDate,
                reportTitle,
                reportSummary,
                status,
                scientificReport,
                supportingDocuments
        );

        return ResponseEntity.ok(response);
    }
}
