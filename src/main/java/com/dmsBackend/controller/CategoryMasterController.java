package com.dmsBackend.controller;

import com.dmsBackend.entity.CategoryMaster;
import com.dmsBackend.exception.ResourceNotFoundException;
import com.dmsBackend.payloads.ApiResponse;
import com.dmsBackend.service.CategoryMasterService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/CategoryMaster")
@Slf4j
public class CategoryMasterController {

    @Autowired
    private CategoryMasterService categoryMasterService;

    // ======================= CREATE =======================
    @PostMapping("/save")
    public ResponseEntity<CategoryMaster> createCategoryMaster(@RequestBody CategoryMaster categoryMaster, HttpServletRequest request) {

        log.info("CONTROLLER → Create Category API | name={}", categoryMaster.getName());

        CategoryMaster saveCategorymaster = this.categoryMasterService.savecategoryMaster(categoryMaster, request);

        log.info("CONTROLLER → Category Created Successfully | id={} name={}",
                saveCategorymaster.getId(), saveCategorymaster.getName());

        return new ResponseEntity<CategoryMaster>(saveCategorymaster, HttpStatus.CREATED);
    }

    // ======================= UPDATE =======================
    @PutMapping("update/{id}")
    public ResponseEntity<CategoryMaster> updateCategoryMaster(@PathVariable Integer id, @RequestBody CategoryMaster categoryMaster, HttpServletRequest request) {

        log.info("CONTROLLER → Update Category API | id={}", id);

        try {
            CategoryMaster updatedcategoryMaster = categoryMasterService.updateCategoryMaster(categoryMaster, id, request);

            log.info("CONTROLLER → Category Updated Successfully | id={} name={}",
                    id, updatedcategoryMaster.getName());

            return new ResponseEntity<>(updatedcategoryMaster, HttpStatus.OK);
        } catch (ResourceNotFoundException e) {

            log.info("CONTROLLER → Update Failed | id={} reason=Not Found", id);

            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    // ======================= DELETE =======================
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<CategoryMaster> deletebyIdCategoryMaster(@PathVariable Integer id) {

        log.info("CONTROLLER → Delete Category API | id={}", id);

        this.categoryMasterService.deleteByIdCategoryMaster(id);

        log.info("CONTROLLER → Category Deleted Successfully | id={}", id);

        return new ResponseEntity(new ApiResponse("CategoryMaster deleted successfully", true), HttpStatus.OK);
    }

    // ======================= FIND ALL =======================
    @GetMapping("/findAll")
    public ResponseEntity<List<CategoryMaster>> findAllBranchMaster() {

        log.info("CONTROLLER → Get All Categories API");

        List<CategoryMaster> allCategoryMasterMaster = this.categoryMasterService.findAllCategoryMaster();

        log.info("CONTROLLER → Retrieved {} Categories", allCategoryMasterMaster.size());

        return new ResponseEntity(allCategoryMasterMaster, HttpStatus.OK);
    }

    // ======================= FIND ACTIVE =======================
    @GetMapping("/findActiveCategory")
    public ResponseEntity<List<CategoryMaster>> findAllActiveCategoryMaster() {

        log.info("CONTROLLER → Get Active Categories API");

        List<CategoryMaster> allActiveCategoryMaster = categoryMasterService.findAllActiveCategoryMaster(true);

        log.info("CONTROLLER → Retrieved {} Active Categories", allActiveCategoryMaster.size());

        return new ResponseEntity<>(allActiveCategoryMaster, HttpStatus.OK);
    }

    // ======================= FIND BY ID =======================
    @GetMapping("/findById/{id}")
    public ResponseEntity<CategoryMaster> findByIdCategoryMaster(@PathVariable Integer id) {

        log.info("CONTROLLER → Get Category By ID API | id={}", id);

        Optional<CategoryMaster> categoryMasterMaster = categoryMasterService.findCategoryMasterById(id);

        if (categoryMasterMaster.isPresent()) {
            log.info("CONTROLLER → Category Found | id={} name={}",
                    id, categoryMasterMaster.get().getName());
            return new ResponseEntity<>(categoryMasterMaster.get(), HttpStatus.OK);
        } else {
            log.info("CONTROLLER → Category Not Found | id={}", id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    // ======================= UPDATE STATUS =======================
    @PutMapping("updatestatus/{id}")
    public ResponseEntity<CategoryMaster> updateCategoryStatus(@PathVariable Integer id, @RequestBody CategoryMaster categoryMaster, HttpServletRequest request) {

        log.info("CONTROLLER → Update Category Status API | id={} newStatus={}",
                id, categoryMaster.isActive());

        try {
            boolean isActive = categoryMaster.isActive();
            CategoryMaster categoryMaster1 = categoryMasterService.updateStatus(id, isActive, request);

            log.info("CONTROLLER → Category Status Updated Successfully | id={} newStatus={}",
                    id, categoryMaster1.isActive());

            return new ResponseEntity<>(categoryMaster1, HttpStatus.OK);
        } catch (ResourceNotFoundException e) {

            log.info("CONTROLLER → Status Update Failed | id={} reason=Not Found", id);

            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}