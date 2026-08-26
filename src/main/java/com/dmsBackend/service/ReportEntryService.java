package com.dmsBackend.service;

import com.dmsBackend.response.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface ReportEntryService {
    List<ReportEntryListItem> getMyPendingReportEntries();
    ReportEntryCaseResponse getCaseForReportEntry(Integer documentHeaderId);
    ApiResponse<MessageResponse> saveReportEntry(
            Integer documentHeaderId,
            String examinationStartDate,
            String examinationEndDate,
            Integer examinationMethodId,
            String observations,
            String scientificOpinion,
            String examinationRemarks,
            String reportDate,
            String reportTitle,
            String reportSummary,
            String status, // "DRAFT" or "SUBMITTED"
            MultipartFile scientificReport,
            List<MultipartFile> supportingDocuments
    );
}