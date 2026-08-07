package com.dmsBackend.P5Archive;

import com.dmsBackend.entity.RetentionPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface P5ApiTransactionsRepository extends JpaRepository<P5ApiTransactions, Long> {
    Optional<P5ApiTransactions>
    findTopByRetentionPolicyAndApiTypeOrderByCreatedAtDesc(
            RetentionPolicy policy,
            String apiType
    );


    List<P5ApiTransactions> findByRetentionPolicyIdAndApiType(Long id, String addfiles);
}
