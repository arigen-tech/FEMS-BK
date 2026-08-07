package com.dmsBackend.repository;

import com.dmsBackend.entity.ApiEndpointType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApiEndpointTypeRepository extends JpaRepository<ApiEndpointType, Integer> {
    Optional<ApiEndpointType> findByName(String name);
    // no extra methods needed for "get all"
}
