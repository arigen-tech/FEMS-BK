package com.dmsBackend.repository;

import com.dmsBackend.entity.BaseMasterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;

/**
 * Generic repository shared by every master. Each concrete master
 * repository just extends this with zero extra code.
 */
@NoRepositoryBean
public interface MasterRepository<T extends BaseMasterEntity> extends JpaRepository<T, Integer> {

    Optional<T> findByNameAndIsActiveTrue(String name);

    List<T> findByIsActiveTrue();
}
