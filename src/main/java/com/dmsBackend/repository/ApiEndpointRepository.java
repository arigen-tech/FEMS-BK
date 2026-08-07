package com.dmsBackend.repository;

import com.dmsBackend.entity.ApiEndpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiEndpointRepository
        extends JpaRepository<ApiEndpoint, Integer> {
    boolean existsByMethodAndEndpoint(String method, String endpoint);

    List<ApiEndpoint> findByEndpointType_Id(Integer endpointTypeId);

    Optional<ApiEndpoint> findByMethodAndEndpoint(String method, String endpoint);
}
