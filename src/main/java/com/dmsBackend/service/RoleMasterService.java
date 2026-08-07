package com.dmsBackend.service;

import com.dmsBackend.entity.RoleMaster;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Optional;

public interface RoleMasterService {

    RoleMaster saveRoleMaster(RoleMaster roleMaster, HttpServletRequest request);
    RoleMaster updateRoleMaster(RoleMaster roleMaster, Integer id,HttpServletRequest request);
    void deleteByIdRoleMaster(Integer id);
    List<RoleMaster> findAllRoleMaster();
    Optional<RoleMaster> findRoleMasterById(Integer id);
    List<RoleMaster> findAllActiveRoleMaster(boolean isActive);
    RoleMaster findRoleByName(String name);

    RoleMaster updateStatus(Integer id, boolean isActive,HttpServletRequest request);


}
