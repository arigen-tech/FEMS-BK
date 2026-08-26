package com.dmsBackend.controller;

import com.dmsBackend.response.*;
import com.dmsBackend.response.ApiResponse;
import com.dmsBackend.service.ReportReviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/report-review")
public class ReportReviewController {

    @Autowired private ReportReviewService reportReviewService;

    @GetMapping("/pending")
    public ResponseEntity<List<ReportReviewListItem>> getPendingReviews() {
        return ResponseEntity.ok(reportReviewService.getPendingReviews());
    }

    @GetMapping("/reviewed/{reviewStatus}")
    public ResponseEntity<List<ReportReviewListItem>> getReviewedReports(@PathVariable String reviewStatus) {
        return ResponseEntity.ok(reportReviewService.getReviewedReports(reviewStatus));
    }

    @GetMapping("/report/{reportEntryId}")
    public ResponseEntity<ReportReviewDetailResponse> getReportForReview(@PathVariable Integer reportEntryId) {
        return ResponseEntity.ok(reportReviewService.getReportForReview(reportEntryId));
    }

    @PostMapping(value = "/review", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<MessageResponse>> reviewReport(
            @RequestParam Integer reportEntryId,
            @RequestParam(required = false) Integer documentDetailId,
            @RequestParam String action,
            @RequestParam(required = false) String comments,
            @RequestParam(required = false) MultipartFile finalReport) {

        ApiResponse<MessageResponse> response = reportReviewService.reviewReport(
                reportEntryId, documentDetailId, action, comments, finalReport);

        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/refer", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<MessageResponse>> referReport(
            @RequestParam Integer reportEntryId,
            @RequestParam(required = false) Integer documentDetailId,
            @RequestParam String toLaboratory,
            @RequestParam(required = false) String fromLaboratory,
            @RequestParam String reason,
            @RequestParam(required = false) MultipartFile supportingDocument) {

        ApiResponse<MessageResponse> response = reportReviewService.referReport(
                reportEntryId, documentDetailId, toLaboratory, fromLaboratory, reason, supportingDocument);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/evidence/{documentHeaderId}")
    public ResponseEntity<List<EvidenceReviewListItem>> getEvidenceForCase(@PathVariable Integer documentHeaderId) {
        return ResponseEntity.ok(reportReviewService.getEvidenceForCase(documentHeaderId));
    }

    @GetMapping("/referrals/outgoing")
    public ResponseEntity<List<ReferralListItem>> getOutgoingReferrals() {
        return ResponseEntity.ok(reportReviewService.getOutgoingReferrals());
    }

    @GetMapping("/referrals/incoming")
    public ResponseEntity<List<ReferralListItem>> getIncomingReferrals() {
        return ResponseEntity.ok(reportReviewService.getIncomingReferrals());
    }

    @PostMapping(value = "/referrals/accept", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<MessageResponse>> acceptReferral(
            @RequestParam Integer documentDetailId) {
        return ResponseEntity.ok(reportReviewService.acceptReferral(documentDetailId));
    }
}