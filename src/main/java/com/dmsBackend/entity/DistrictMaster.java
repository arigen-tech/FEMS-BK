package com.dmsBackend.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** District, depends on State (master_state.id). */
@Entity
@Table(name = "master_district")
@AttributeOverride(name = "parentId", column = @Column(name = "state_id"))
@Getter
@Setter
public class DistrictMaster extends ParentedMasterEntity implements CodedMaster {

    @Column(name = "code")
    private String code;
}