package com.dmsBackend.entity;

public enum DocApprovalStatus {
    PENDING("Pending"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    PARTIALLY_PENDING("Partially Pending"),
    PARTIALLY_APPROVED("Partially Approved"),
    PARTIALLY_REJECT("Partially Rejected");

    private final String displayName;
    DocApprovalStatus(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
}

