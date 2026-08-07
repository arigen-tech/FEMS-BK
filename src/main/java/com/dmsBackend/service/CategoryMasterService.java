package com.dmsBackend.service;

import com.dmsBackend.entity.BranchMaster;
import com.dmsBackend.entity.CategoryMaster;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
@Service
public interface CategoryMasterService {
    CategoryMaster savecategoryMaster(CategoryMaster categoryMaster, HttpServletRequest request);
    CategoryMaster updateCategoryMaster(CategoryMaster categoryMaster,Integer id, HttpServletRequest request);
    void deleteByIdCategoryMaster(Integer id);
    List<CategoryMaster> findAllCategoryMaster();

    List<CategoryMaster> findAllActiveCategoryMaster(boolean active);

    Optional<CategoryMaster> findCategoryMasterById(Integer id);
    CategoryMaster findByIdCate(Integer id);
    CategoryMaster updateStatus(Integer id, boolean  isActive, HttpServletRequest request );
}