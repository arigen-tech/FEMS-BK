package com.dmsBackend.repository;

import com.dmsBackend.entity.LanguageMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LanguageMasterRepository extends JpaRepository<LanguageMaster, Long> {

    List<LanguageMaster> findByIsActiveTrue();

    Optional<LanguageMaster> findByCodeAndIsActiveTrue(String code);
}
