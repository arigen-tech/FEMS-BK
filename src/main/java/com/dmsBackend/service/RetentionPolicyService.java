package com.dmsBackend.service;

import com.dmsBackend.P5Archive.P5DashboardRes1;
import com.dmsBackend.entity.DocumentDetails;
import com.dmsBackend.entity.RetentionPolicy;
import com.dmsBackend.response.NewRetentionPolicyRequest;
import com.dmsBackend.response.RetentionPolicyRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface RetentionPolicyService {


    List<P5DashboardRes1> findAll(Long branchId, Long departmentId);

    @Transactional
    RetentionPolicy updateNewPolicy(Long id, NewRetentionPolicyRequest req);


    @Transactional
    RetentionPolicy NewCreatePolicy(NewRetentionPolicyRequest newRequest);

    public List<RetentionPolicy> findAll();



}
