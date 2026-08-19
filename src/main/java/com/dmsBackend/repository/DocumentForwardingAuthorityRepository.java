package com.dmsBackend.repository;


import com.dmsBackend.entity.DocumentForwardingAuthority;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DocumentForwardingAuthorityRepository extends JpaRepository<DocumentForwardingAuthority, Integer> {

    // Used on update to find the one row belonging to this document
    // (one-to-one in practice, even though the FK itself isn't unique-constrained)
    Optional<DocumentForwardingAuthority> findByDocumentHeader_Id(Integer documentHeaderId);
}
