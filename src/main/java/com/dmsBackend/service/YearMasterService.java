package com.dmsBackend.service;

import com.dmsBackend.entity.CategoryMaster;
import com.dmsBackend.entity.RoleMaster;
import com.dmsBackend.entity.YearMaster;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public interface YearMasterService {
    YearMaster saveYearMaster(YearMaster yearMaster, HttpServletRequest request);
    YearMaster updateYearMaster(YearMaster yearMaster,Integer id,HttpServletRequest request);

    List<YearMaster> findAllActiveYearMaster(int isActive);

    void deleteByIdYearMaster(Integer id);
    List<YearMaster> findAllYearMaster();
    Optional<YearMaster> findYearMasterById(Integer id);
    YearMaster findByIdyear(Integer id);
    YearMaster updateStatus(Integer id, Integer isActive,HttpServletRequest request);


}
