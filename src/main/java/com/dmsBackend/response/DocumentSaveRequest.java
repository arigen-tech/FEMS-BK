package com.dmsBackend.response;

import com.dmsBackend.entity.DocumentHeader;
import lombok.Data;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

public class DocumentSaveRequest {
    private DocumentHeader documentHeader;
    private List<FilePathVersion> filePaths;

    public List<MetadataRequest> getMetadata() {
        return metadata;
    }

    public void setMetadata(List<MetadataRequest> metadata) {
        this.metadata = metadata;
    }

    private List<MetadataRequest> metadata;

    public List<Long> getDeletedMetaDataIds() {
        return deletedMetaDataIds;
    }

    public void setDeletedMetaDataIds(List<Long> deletedMetaDataIds) {
        this.deletedMetaDataIds = deletedMetaDataIds;
    }

    private List<Long> deletedMetaDataIds;

    public ForwardingAuthorityRequest getForwardingAuthority() {
        return forwardingAuthority;
    }

    public void setForwardingAuthority(ForwardingAuthorityRequest forwardingAuthority) {
        this.forwardingAuthority = forwardingAuthority;
    }

    private ForwardingAuthorityRequest forwardingAuthority;


    // Getters and setters
    public DocumentHeader getDocumentHeader() {
        return documentHeader;
    }

    public void setDocumentHeader(DocumentHeader documentHeader) {
        this.documentHeader = documentHeader;
    }

    public List<FilePathVersion> getFilePaths() {
        return filePaths;
    }

    public void setFilePaths(List<FilePathVersion> filePaths) {
        this.filePaths = filePaths;
    }

    @Data
    public static class FilePathVersion {
        private String path;
        private String version;

        private String mimeType;

        private String displayName;

        private Integer waitingRoomId; // Add this
        private Boolean isWaitingRoomFile; // Add this

        public Integer getPageCounts() {
            return pageCounts;
        }

        public void setPageCounts(Integer pageCounts) {
            this.pageCounts = pageCounts;
        }

        private Integer pageCounts;

        public String getMimeType() {
            return mimeType;
        }

        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }

        public String getFileType() {
            return fileType;
        }

        public void setFileType(String fileType) {
            this.fileType = fileType;
        }

        public String getFileSizeBytes() {
            return fileSizeBytes;
        }

        public void setFileSizeBytes(String fileSizeBytes) {
            this.fileSizeBytes = fileSizeBytes;
        }

        public String getFileSizeHuman() {
            return fileSizeHuman;
        }

        public void setFileSizeHuman(String fileSizeHuman) {
            this.fileSizeHuman = fileSizeHuman;
        }

        private String fileType;
        private String fileSizeBytes;
        private String fileSizeHuman;

        public Long getYearId() {
            return yearId;
        }

        public void setYearId(Long yearId) {
            this.yearId = yearId;
        }

        private Long yearId;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }

    @Data
    public static class MetadataRequest {
        private long id;
        private String key;
        private String value;
    }

    // Matches the "forwardingAuthority" object shape sent by
    // ForwardingAuthorityDetails.jsx / DocumentManagement.jsx's payload.
    @Data
    public static class ForwardingAuthorityRequest {
        private Integer forwardingAuthorityTypeId;
        private String authorityName;
        private String designation;
        private String organisation;
        private Integer districtId;
        private Integer cityId; // not yet collected by the frontend form; will save null until added
        private String address;
        private String contactNumber;
        private String email;
        private String forwardingLetterNumber;
        private String forwardingLetterPath;
        private Timestamp forwardingDate;
        private Integer modeOfSubmissionId;
        private String courierAgency;
        private String awbConsignmentNumber;
        private Timestamp bookingDate;
        private Timestamp dispatchDate;
        private Timestamp expectedDeliveryDate;
        private Timestamp actualDeliveryDate;
        private String parcelId;
        private String parcelNumber;
        private Integer numberOfExhibits;
        private Integer packageTypeId;
        private String sealNumber;
        private String sealDescription;
        private String sealCondition;
        private String packageCondition;
        private Timestamp receivedDate;
        private String receivedTime;
        private String receivedBy;
        private String remarks;
    }

}