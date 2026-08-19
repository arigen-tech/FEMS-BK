package com.dmsBackend.entity;

/**
 * Implemented by masters that cascade from a parent dropdown
 * (District -> State, City -> District, EvidenceType -> Category).
 */
public interface ParentAware {
    Integer getParentId();
    void setParentId(Integer parentId);
}
