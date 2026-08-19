package com.dmsBackend.repository;

import com.dmsBackend.entity.ParentedMasterEntity;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;

/**
 * Adds parent-based lookups (e.g. districts for a given state)
 * on top of MasterRepository. Used by District, City, EvidenceType.
 */
@NoRepositoryBean
public interface ParentedMasterRepository<T extends ParentedMasterEntity> extends MasterRepository<T> {

    List<T> findByParentIdAndIsActiveTrue(Integer parentId);

    List<T> findByParentId(Integer parentId);
}
