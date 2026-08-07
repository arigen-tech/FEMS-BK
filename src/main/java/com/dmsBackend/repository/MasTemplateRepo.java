package com.dmsBackend.repository;

import com.dmsBackend.entity.MasTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MasTemplateRepo extends JpaRepository<MasTemplate, Long> {
    List<MasTemplate> findByStatusIgnoreCase(String status);
    List<MasTemplate> findByStatusInIgnoreCase(List<String> statuses);

}