// MasterType.java — 4 entries added, everything else unchanged
package com.dmsBackend.entity;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum MasterType {

    CASE_TYPE("case-type", "caseTypeMasterService"),
    CRIME_TYPE("crime-type", "crimeTypeMasterService"),
    STATE("state", "stateMasterService"),
    DISTRICT("district", "districtMasterService"),
    CITY("city", "cityMasterService"),
    PRIORITY("priority", "priorityMasterService"),
    EVIDENCE_TYPE("evidence-type", "evidenceTypeMasterService"),
    FORWARDING_AUTHORITY_TYPE("forwarding-authority-type", "forwardingAuthorityTypeMasterService"),
    MODE_OF_SUBMISSION("mode-of-submission", "modeOfSubmissionMasterService"),
    PACKAGE_TYPE("package-type", "packageTypeMasterService"),
    PURPOSE("purpose", "purposeMasterService"),
    NATURE_OF_EXAMINATION("nature-of-examination", "natureOfExaminationMasterService"),
    SEAL_STATUS("seal-status", "sealStatusMasterService"),
    EXAMINATION_METHOD("examination-method", "examinationMethodMasterService"),

    PARCEL_CONDITION("parcel-condition", "parcelConditionMasterService");

    private final String key;
    private final String beanName;

    MasterType(String key, String beanName) {
        this.key = key;
        this.beanName = beanName;
    }

    public String getKey() {
        return key;
    }

    public String getBeanName() {
        return beanName;
    }

    public static MasterType fromKey(String key) {
        return Arrays.stream(values())
                .filter(t -> t.key.equalsIgnoreCase(key))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown master type '" + key + "'. Valid types: " +
                                Arrays.stream(values()).map(MasterType::getKey).collect(Collectors.joining(", "))));
    }
}