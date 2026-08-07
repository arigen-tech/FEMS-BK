package com.dmsBackend.repository;

import com.dmsBackend.entity.DocumentMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentMetadataRepository
        extends JpaRepository<DocumentMetadata, Long> {
}
