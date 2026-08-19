package com.dmsBackend.service.Impl;

import com.dmsBackend.entity.DocApprovalStatus;
import com.dmsBackend.entity.DocumentDetails;
import com.dmsBackend.entity.DocumentHeader;
import com.dmsBackend.repository.DocumentDetailsRepository;
import com.dmsBackend.service.DocumentsAuditLogService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;

@Slf4j
@Service
public class DocHeaderStatusService {

    private final DocumentDetailsRepository detailsRepo;
    private final DocumentsAuditLogService auditService;

    public DocHeaderStatusService(DocumentDetailsRepository detailsRepo,
                                  DocumentsAuditLogService auditService) {
        this.detailsRepo = detailsRepo;
        this.auditService = auditService;
    }

    /**
     * LEGACY: recalculates header status from detail statuses.
     * Simplified for the 3-state model (no PARTIALLY_* states).
     * Kept only so older callers (e.g. DocumentDetailsServiceImpl.updateDetailStatus)
     * still compile; the primary approve/reject flow now goes through
     * applyHeaderDecision() below.
     */
    @Transactional
    public void recalcAndUpdateHeaderStatus(DocumentHeader header, String performedBy) {
        Integer headerId = header.getId();

        long total = detailsRepo.countByDocumentHeaderId(headerId);

        if (total == 0) {
            header.setApprovalStatus(DocApprovalStatus.PENDING);
            header.setIfPending(true);
            header.setIfApproved(false);
            header.setIfRejected(false);
            return;
        }

        long pending  = detailsRepo.countByDocumentHeaderIdAndStatus(headerId, DocApprovalStatus.PENDING);
        long approved = detailsRepo.countByDocumentHeaderIdAndStatus(headerId, DocApprovalStatus.APPROVED);
        long rejected = detailsRepo.countByDocumentHeaderIdAndStatus(headerId, DocApprovalStatus.REJECTED);

        header.setIfPending(pending > 0);
        header.setIfApproved(approved > 0);
        header.setIfRejected(rejected > 0);

        DocApprovalStatus newStatus;
        if (approved == total) {
            newStatus = DocApprovalStatus.APPROVED;
        } else if (rejected == total) {
            newStatus = DocApprovalStatus.REJECTED;
        } else {
            newStatus = DocApprovalStatus.PENDING;
        }

        if (header.getApprovalStatus() != newStatus) {
            header.setApprovalStatus(newStatus);
            header.setUpdatedBy(performedBy);
            header.setUpdatedOn(new Timestamp(System.currentTimeMillis()));
        }
    }

    /**
     * PRIMARY FLOW: header-level approve/reject.
     * Sets the header's status directly and cascades the same status
     * to every detail row under it, so the file list stays consistent
     * with the case-level decision.
     */
    @Transactional
    public void applyHeaderDecision(DocumentHeader header,
                                    DocApprovalStatus newStatus,
                                    String rejectionReason,
                                    String performedBy) {

        Timestamp now = new Timestamp(System.currentTimeMillis());

        header.setApprovalStatus(newStatus);
        header.setApprovalStatusBy(performedBy);
        header.setApprovalStatusOn(now);
        header.setUpdatedBy(performedBy);
        header.setUpdatedOn(now);

        header.setIfPending(newStatus == DocApprovalStatus.PENDING);
        header.setIfApproved(newStatus == DocApprovalStatus.APPROVED);
        header.setIfRejected(newStatus == DocApprovalStatus.REJECTED);

        List<DocumentDetails> details = detailsRepo.findByDocumentHeaderId(header.getId());
        for (DocumentDetails d : details) {
            d.setStatus(newStatus);
            d.setUpdatedOn(now);
            d.setUpdatedBy(performedBy);

            if (newStatus == DocApprovalStatus.APPROVED) {
                d.setApprovedOn(now);
                d.setApprovedBy(performedBy);
                d.setRejectionReason(null);
            } else if (newStatus == DocApprovalStatus.REJECTED) {
                d.setApprovedOn(null);
                d.setApprovedBy(null);
                d.setRejectionReason(rejectionReason);
            } else {
                d.setApprovedOn(null);
                d.setApprovedBy(null);
                d.setRejectionReason(null);
            }
        }
        detailsRepo.saveAll(details);

        log.info("Header decision applied | headerId={} status={} detailCount={} by={}",
                header.getId(), newStatus, details.size(), performedBy);
    }
}