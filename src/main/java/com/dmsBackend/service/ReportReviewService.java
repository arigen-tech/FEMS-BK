package com.dmsBackend.service;

import com.dmsBackend.response.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ReportReviewService {

    List<ReportReviewListItem> getPendingReviews();

    List<ReportReviewListItem> getReviewedReports(String reviewStatus);

    ReportReviewDetailResponse getReportForReview(Integer reportEntryId);

    ApiResponse<MessageResponse> reviewReport(
            Integer reportEntryId,
            Integer documentDetailId,
            String action,
            String comments,
            MultipartFile finalReport);

    ApiResponse<MessageResponse> referReport(
            Integer reportEntryId,
            Integer documentDetailId,
            String toLaboratory,
            String fromLaboratory,
            String reason,
            MultipartFile supportingDocument);

    List<EvidenceReviewListItem> getEvidenceForCase(Integer documentHeaderId);

    List<ReferralListItem> getOutgoingReferrals();

    List<ReferralListItem> getIncomingReferrals();

    ApiResponse<MessageResponse> acceptReferral(Integer documentDetailId);
}