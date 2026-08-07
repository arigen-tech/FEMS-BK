package com.dmsBackend.P5Archive;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface P5RequestResponceRepository extends JpaRepository<P5RequestResponce, Long> {
    Optional<P5RequestResponce>
    findTopByRetentionPolicyIdOrderByIdDesc(Long retentionPolicyId);

    P5RequestResponce findByLtoRetentionJobId(Long ltoJobId);

    P5RequestResponce findByRetentionPolicyId(Long id);
}
