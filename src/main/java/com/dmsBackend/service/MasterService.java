package com.dmsBackend.service;

import com.dmsBackend.entity.BaseMasterEntity;
import com.dmsBackend.response.MasterRequest;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * One service contract for every master type added for
 * Register Case & Evidence.
 */
public interface MasterService<T extends BaseMasterEntity> {

    T save(MasterRequest request, HttpServletRequest httpRequest);


    // ======================= UPDATE =======================
    T update(MasterRequest request, Integer id, HttpServletRequest httpRequest);

    List<T> findAll(int flag);

    /** For cascading dropdowns e.g. districts of a state. Throws for masters with no parent. */
    List<T> findByParent(Integer parentId, int flag);

//    T findById(Long id);

    T updateStatus(Integer id, Boolean isActive, HttpServletRequest request);

    // ======================= GET BY ID =======================
    T findById(Integer id);


}
