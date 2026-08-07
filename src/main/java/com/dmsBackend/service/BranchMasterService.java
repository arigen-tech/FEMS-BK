package com.dmsBackend.service;

import com.dmsBackend.entity.BranchMaster;
import com.dmsBackend.entity.RoleMaster;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;


public interface BranchMasterService {
    BranchMaster saveBranchMaster(BranchMaster branchMaster, HttpServletRequest request);
    BranchMaster updateBranchMaster(BranchMaster branchMaster,Integer id,HttpServletRequest request);
     void deleteByIdBranchMaster(Integer id);
     List<BranchMaster> findAllBranchMaster();

    List<BranchMaster> findAllActiveBranchMaster(Integer isActive);

    Optional<BranchMaster> findBranchMasterById(Integer id);
    BranchMaster findByIdBran(Integer id);
    BranchMaster updateStatus(Integer id, Integer isActive,HttpServletRequest request);



}
