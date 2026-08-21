// SealStatusMaster.java
package com.dmsBackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "master_seal_status")
@Getter
@Setter
public class SealStatusMaster extends BaseMasterEntity implements HasDefaultParcelCondition {

    @Column(name = "default_parcel_condition_id")
    private Integer defaultParcelConditionId;

    @Column(name = "requires_verification_remarks")
    private Boolean requiresVerificationRemarks = false;
}
