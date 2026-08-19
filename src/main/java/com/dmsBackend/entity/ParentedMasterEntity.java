package com.dmsBackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Base for masters that depend on a parent master.
 * Subclasses map the physical FK column via @AttributeOverride, e.g.:
 *   @AttributeOverride(name = "parentId", column = @Column(name = "state_id"))
 */
@MappedSuperclass
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class ParentedMasterEntity extends BaseMasterEntity implements ParentAware {

    @Column(name = "parent_id")
    private Integer parentId;
}
