package com.dmsBackend.service.Impl;

import com.dmsBackend.entity.CategoryMaster;
import com.dmsBackend.exception.ResourceNotFoundException;
import com.dmsBackend.payloads.Helper;
import com.dmsBackend.repository.CategoryMasterRepository;
import com.dmsBackend.service.CategoryMasterService;
import com.dmsBackend.utils.AuditLogUtil;
import com.dmsBackend.utils.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class CategoryMasterServiceImpl implements CategoryMasterService {

    @Autowired
    CategoryMasterRepository categoryMasterRepository;

    @Autowired
    CurrentUser currentUser;

    @Autowired
    AuditLogUtil auditLogUtil;

    // ======================= SAVE =======================
    @Override
    public CategoryMaster savecategoryMaster(CategoryMaster categoryMaster, HttpServletRequest request) {

        log.info("API CALL → Save Category | name={}", categoryMaster.getName());

        categoryMaster.setActive(true);
        categoryMaster.setCreatedOn(Helper.getCurrentTimeStamp());
        categoryMaster.setUpdatedOn(Helper.getCurrentTimeStamp());

        try {
            CategoryMaster savedCategory = categoryMasterRepository.save(categoryMaster);

            log.info("SUCCESS → Category Saved | id={} name={}",
                    savedCategory.getId(), savedCategory.getName());

            auditLogUtil.logAction(
                    currentUser.getCurrentEmployeeOrThrow(),
                    "Categories",
                    "Create",
                    "Success",
                    savedCategory.getId(),
                    savedCategory.getName(),
                    savedCategory.getId(),
                    Map.of("name", savedCategory.getName()),
                    request
            );

            return savedCategory;
        } catch (Exception ex) {

            log.info("FAILED → Save Category | name={} reason={}",
                    categoryMaster.getName(), ex.getMessage());

            auditLogUtil.logAction(
                    currentUser.getCurrentEmployeeOrThrow(),
                    "Categories",
                    "Create",
                    "Failure",
                    null,
                    categoryMaster.getName(),
                    null,
                    Map.of("error", ex.getMessage()),
                    request
            );
            throw ex;
        }
    }

    // ======================= UPDATE =======================
    @Override
    public CategoryMaster updateCategoryMaster(CategoryMaster categoryMaster, Integer id, HttpServletRequest request) {

        log.info("API CALL → Update Category | id={}", id);

        Optional<CategoryMaster> categoryMasterOptional = categoryMasterRepository.findById(id);

        if (categoryMasterOptional.isPresent()) {
            CategoryMaster existing = categoryMasterOptional.get();

            log.info("Updating Category | id={} oldName={} newName={}",
                    id, existing.getName(), categoryMaster.getName());

            Map<String, Object> previousData = Map.of(
                    "name", existing.getName()
            );

            existing.setName(categoryMaster.getName());
            existing.setUpdatedOn(Helper.getCurrentTimeStamp());

            CategoryMaster updatedCategory = categoryMasterRepository.save(existing);

            log.info("SUCCESS → Category Updated | id={} name={}",
                    updatedCategory.getId(), updatedCategory.getName());

            auditLogUtil.logAction(
                    currentUser.getCurrentEmployeeOrThrow(),
                    "Categories",
                    "Update",
                    "Success",
                    updatedCategory.getId(),
                    updatedCategory.getName(),
                    updatedCategory.getId(),
                    previousData,
                    request
            );

            return updatedCategory;
        }

        log.info("FAILED → Update Category | id={} reason=Not Found", id);

        auditLogUtil.logAction(
                currentUser.getCurrentEmployeeOrThrow(),
                "Categories",
                "Update",
                "Failure",
                id,
                null,
                id,
                Map.of("error", "CategoryMaster not found"),
                request
        );

        throw new ResourceNotFoundException("CategoryMaster not found for ", "Id", id);
    }

    // ======================= DELETE =======================
    @Override
    public void deleteByIdCategoryMaster(Integer id) {

        log.info("API CALL → Delete Category | id={}", id);

        categoryMasterRepository.deleteById(id);

        log.info("SUCCESS → Category Deleted | id={}", id);
    }

    // ======================= FIND ALL =======================
    @Override
    public List<CategoryMaster> findAllCategoryMaster() {

        log.info("API CALL → Get All Categories");

        return categoryMasterRepository.findAllCategoryMasterOrdered();
    }

    // ======================= FIND ACTIVE =======================
    @Override
    public List<CategoryMaster> findAllActiveCategoryMaster(boolean active) {

        log.info("API CALL → Get Active Categories | active={}", active);

        return categoryMasterRepository.findByActive(active);
    }

    // ======================= FIND BY ID =======================
    @Override
    public Optional<CategoryMaster> findCategoryMasterById(Integer id) {

        log.info("API CALL → Get Category By ID | id={}", id);

        return categoryMasterRepository.findById(id);
    }

    @Override
    public CategoryMaster findByIdCate(Integer id) {

        log.info("API CALL → Get Category (Strict) | id={}", id);

        return categoryMasterRepository.findById(id)
                .orElseThrow(() -> {
                    log.info("FAILED → Get Category | id={} reason=Not Found", id);
                    return new ResourceNotFoundException("CategoryMaster not found", "Id", id);
                });
    }

    // ======================= STATUS UPDATE =======================
    @Override
    public CategoryMaster updateStatus(Integer id, boolean isActive, HttpServletRequest request) {

        log.info("API CALL → Category Status Update | id={} newStatus={}", id, isActive);

        CategoryMaster categoryMaster = categoryMasterRepository.findById(id)
                .orElseThrow(() -> {

                    log.info("FAILED → Status Update | id={} reason=Not Found", id);

                    auditLogUtil.logAction(
                            currentUser.getCurrentEmployeeOrThrow(),
                            "Categories",
                            "StatusUpdate",
                            "Failure",
                            id,
                            null,
                            id,
                            Map.of("error", "CategoryMaster not found"),
                            request
                    );

                    return new ResourceNotFoundException("CategoryMaster not found", "id", id);
                });

        boolean oldStatus = categoryMaster.isActive();

        categoryMaster.setActive(isActive);
        categoryMaster.setUpdatedOn(Helper.getCurrentTimeStamp());

        CategoryMaster updatedCategory = categoryMasterRepository.save(categoryMaster);

        log.info("SUCCESS → Category Status Updated | id={} oldStatus={} newStatus={}",
                id, oldStatus, isActive);

        auditLogUtil.logAction(
                currentUser.getCurrentEmployeeOrThrow(),
                "Categories",
                "StatusUpdate",
                "Success",
                updatedCategory.getId(),
                updatedCategory.getName(),
                updatedCategory.getId(),
                Map.of("isActive", oldStatus),
                request
        );

        return updatedCategory;
    }
}