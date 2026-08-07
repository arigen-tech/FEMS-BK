package com.dmsBackend.response;

import com.dmsBackend.entity.DocumentDetails;
import com.dmsBackend.entity.DocumentHeader;
import com.dmsBackend.entity.DocumentShare;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class DocumentShareResponse {
    private Long shareId;
    private String documentName;
    private String sharedBy;
    private List<String> sharedTo;
    private LocalDateTime createdAt;
    private LocalDateTime endTime;
    private String status;
    private List<ShareResult> results;
    private Integer totalFilesShared; // Add this field
    private Integer totalDocuments;   // Add this field

    @Getter
    @Setter
    public static class ShareResult {
        private String employeeName;
        private boolean success;
        private String message;
        private Long documentShareId;
    }

    @Getter
    @Setter
    public static class ShareInfoResponse {
        private Long id;
        private String sharedByName;
        private String sharedToName;
        private LocalDateTime sharedDate;
        private LocalDateTime endTime;
        private boolean isExpired;
        private DocumentHeaderResponse documentHeader;
//        private DocumentDetailsResponse documentDetails;

        // Add these new fields
        private Integer totalFilesShared;
        private List<String> sharedFileNames;
        private List<Long> shareIds;
        private Integer documentHeaderId;
        private String documentName;
        private String documentHeaderName;

        // Static factory method
        public static ShareInfoResponse fromEntity(DocumentShare share) {
            if (share == null) {
                return null;
            }

            ShareInfoResponse response = new ShareInfoResponse();
            response.setId(share.getId());

            // Set document name from details if available
            if (share.getDocumentDetails() != null) {
                response.setDocumentName(share.getDocumentDetails().getDocName());
            }

            // Set shared by/to names
            if (share.getSharedBy() != null) {
                response.setSharedByName(share.getSharedBy().getName());
            }

            if (share.getSharedTo() != null) {
                response.setSharedToName(share.getSharedTo().getName());
            }

            response.setSharedDate(share.getCreatedAt());
            response.setEndTime(share.getEndTime());
            response.setExpired(share.getEndTime() != null &&
                    share.getEndTime().isBefore(LocalDateTime.now()));

            // Add DocumentHeader information
            if (share.getDocumentHeader() != null) {
                response.setDocumentHeader(DocumentHeaderResponse.fromEntity(share.getDocumentHeader()));
                response.setDocumentHeaderId(share.getDocumentHeader().getId());
                response.setDocumentHeaderName(share.getDocumentHeader().getTitle());
            }

            // Add DocumentDetails information
//            if (share.getDocumentDetails() != null) {
//                response.setDocumentDetails(DocumentDetailsResponse.fromEntity(share.getDocumentDetails()));
//            }

            return response;
        }

        // Setter methods for the new fields
        public void setTotalFilesShared(Integer totalFilesShared) {
            this.totalFilesShared = totalFilesShared;
        }

        public void setSharedFileNames(List<String> sharedFileNames) {
            this.sharedFileNames = sharedFileNames;
        }

        public void setShareIds(List<Long> shareIds) {
            this.shareIds = shareIds;
        }
    }

    @Getter
    @Setter
    public static class DocumentHeaderResponse {
        private Long id;
        private String docName;
        private String title; // Add this field
        private String subject; // Add this field
        private String fileNo; // Add this field
        private String categoryName;
        private String branchName;
        private String departmentName;
        private List<DocumentDetailsResponse> documentDetails;
        private LocalDateTime createdOn;
        private String createdBy;
        private LocalDateTime updatedOn;
        private String updatedBy;


        public static DocumentHeaderResponse fromEntity(DocumentHeader header) {
            if (header == null) {
                return null;
            }

            DocumentHeaderResponse response = new DocumentHeaderResponse();
            response.setId(Long.valueOf(header.getId()));
            response.setTitle(header.getTitle());
            response.setSubject(header.getSubject());
            response.setFileNo(header.getFileNo());
            response.setBranchName(header.getBranchMaster().getName());
            response.setDepartmentName(header.getDepartmentMaster().getName());

            response.setCategoryName(header.getCategoryMaster().getName());


            // Safely handle document details
            if (header.getDocumentDetails() != null && !header.getDocumentDetails().isEmpty()) {
                try {
                    response.setDocumentDetails(header.getDocumentDetails().stream()
                            .map(DocumentDetailsResponse::fromEntity)
                            .collect(Collectors.toList()));
                } catch (Exception e) {
                    // Log error but don't fail
                    System.err.println("Error processing document details: " + e.getMessage());
                    response.setDocumentDetails(List.of());
                }
            } else {
                response.setDocumentDetails(List.of());
            }

            response.setCreatedOn(header.getCreatedOn().toLocalDateTime());
            response.setCreatedBy(header.getCreatedBy());
            response.setUpdatedOn(header.getUpdatedOn().toLocalDateTime());
            response.setUpdatedBy(header.getUpdatedBy());

            return response;
        }
    }

    @Getter
    @Setter
    public static class DocumentDetailsResponse {
        private Integer id;
        private String docName;
        private String path;
        private String version;
        private String status;
        private LocalDateTime createdOn;
        private String createdBy;
        private LocalDateTime updatedOn;
        private String updatedBy;
        private LocalDateTime approvedOn;
        private String approvedBy;

        public static DocumentDetailsResponse fromEntity(DocumentDetails details) {
            if (details == null) {
                return null;
            }

            DocumentDetailsResponse response = new DocumentDetailsResponse();
            response.setId(details.getId());
            response.setDocName(details.getDocName());
            response.setPath(details.getPath());
            response.setVersion(details.getVersion());
            response.setStatus(details.getStatus() != null ? details.getStatus().name() : null);

            response.setCreatedOn(details.getCreatedOn().toLocalDateTime());
            response.setCreatedBy(details.getCreatedBy());
            response.setUpdatedOn(details.getUpdatedOn().toLocalDateTime());
            response.setUpdatedBy(details.getUpdatedBy());
            response.setApprovedOn(details.getApprovedOn().toLocalDateTime());
            response.setApprovedBy(details.getApprovedBy());

            return response;
        }
    }

    // Setter methods for the new fields
    public void setTotalFilesShared(Integer totalFilesShared) {
        this.totalFilesShared = totalFilesShared;
    }

    public void setTotalDocuments(Integer totalDocuments) {
        this.totalDocuments = totalDocuments;
    }
}