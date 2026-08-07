package com.dmsBackend.repository;

import com.dmsBackend.entity.RoleTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleTemplateRepo extends JpaRepository<RoleTemplate, Long> {
    List<RoleTemplate> findByRoleId(Long roleId);

    @Query("SELECT r FROM RoleTemplate r WHERE r.roleId = :roleId AND r.template.id = :templateId")
    Optional<RoleTemplate> findByRoleIdAndTemplateId(Long roleId, Long templateId);

    List<RoleTemplate> findByRoleIdAndStatusIgnoreCase(Long roleId, String status);

}
