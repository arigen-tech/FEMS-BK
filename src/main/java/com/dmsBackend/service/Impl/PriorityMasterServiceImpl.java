package com.dmsBackend.service.Impl;

import com.dmsBackend.entity.PriorityMaster;
import com.dmsBackend.repository.PriorityMasterRepository;
import com.dmsBackend.repository.MasterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("priorityMasterService")
public class PriorityMasterServiceImpl extends GenericMasterServiceImpl<PriorityMaster> {

    @Autowired
    private PriorityMasterRepository repository;

    @Override
    protected MasterRepository<PriorityMaster> getRepository() {
        return repository;
    }

    @Override
    protected String getEntityLabel() {
        return "Priority";
    }

    @Override
    protected PriorityMaster newInstance() {
        return new PriorityMaster();
    }
}
