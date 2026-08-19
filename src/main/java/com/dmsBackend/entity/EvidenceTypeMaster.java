package com.dmsBackend.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Evidence Type, depends on Evidence Category (category_id). */
@Entity
@Table(name = "master_evidence_type")
@AttributeOverride(name = "parentId", column = @Column(name = "category_id"))
public class EvidenceTypeMaster extends ParentedMasterEntity {
}
